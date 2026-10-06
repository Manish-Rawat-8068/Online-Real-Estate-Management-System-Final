package com.manishrawat.realestate.thread;

import com.manishrawat.realestate.util.DBConnection;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Applies small, idempotent compatibility migrations before requests are served. */
@WebListener
public final class SchemaMigrationListener implements ServletContextListener {
    private static final Logger LOG = Logger.getLogger(SchemaMigrationListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent event) {
        try (Connection connection = DBConnection.getConnection()) {
            List<String> propertyColumns = columns(connection, "properties");
            if (!propertyColumns.contains("rent")) {
                String legacyRentColumn = firstPresent(propertyColumns, List.of("price", "monthly_rent",
                        "rent_amount", "rent_price", "rent_per_month", "monthly_price",
                        "price_per_month", "rental_price", "rental_amount", "amount"));
                if (legacyRentColumn == null) {
                    throw new SQLException("The properties table has neither rent nor a recognized legacy rent column.");
                }
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("ALTER TABLE properties ADD COLUMN rent DECIMAL(12,2) NULL");
                }
                String copyLegacyRent = "UPDATE properties SET rent = `" + legacyRentColumn + "` WHERE rent IS NULL";
                try (Statement statement = connection.createStatement()) {
                    int updated = statement.executeUpdate(copyLegacyRent);
                    statement.executeUpdate("ALTER TABLE properties MODIFY rent DECIMAL(12,2) NOT NULL");
                    LOG.info("Added properties.rent and migrated " + updated + " existing value(s) from "
                            + legacyRentColumn + ".");
                }
            }

            boolean hasRent;
            String columnQuery = "SELECT COUNT(*) FROM information_schema.columns "
                    + "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?";
            try (PreparedStatement statement = connection.prepareStatement(columnQuery)) {
                statement.setString(1, "rental_agreements");
                statement.setString(2, "rent");
                try (ResultSet result = statement.executeQuery()) {
                    result.next();
                    hasRent = result.getInt(1) > 0;
                }
            }

            if (!hasRent) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("ALTER TABLE rental_agreements "
                            + "ADD COLUMN rent DECIMAL(12,2) NULL");
                }
            }

            String backfill = "UPDATE rental_agreements g "
                    + "JOIN rental_applications a ON a.id = g.application_id "
                    + "JOIN properties p ON p.id = a.property_id "
                    + "SET g.rent = p.rent WHERE g.rent IS NULL OR g.rent = 0.00";
            try (PreparedStatement statement = connection.prepareStatement(backfill)) {
                int updated = statement.executeUpdate();
                if (updated > 0) {
                    LOG.info("Backfilled rent for " + updated + " existing rental agreement(s).");
                }
            }
            if (!hasRent) {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("ALTER TABLE rental_agreements MODIFY rent DECIMAL(12,2) NOT NULL");
                }
                LOG.info("Added the missing rental_agreements.rent column and migrated existing agreements.");
            }
        } catch (SQLException exception) {
            LOG.log(Level.SEVERE, "Could not apply the rental agreement database compatibility migration.", exception);
            throw new IllegalStateException("Rental agreement database migration failed.", exception);
        }
    }

    private List<String> columns(Connection connection, String table) throws SQLException {
        List<String> result = new ArrayList<>();
        String query = "SELECT column_name FROM information_schema.columns "
                + "WHERE table_schema = DATABASE() AND table_name = ? ORDER BY ordinal_position";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, table);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    result.add(rows.getString(1).toLowerCase(java.util.Locale.ROOT));
                }
            }
        }
        return result;
    }

    private String firstPresent(List<String> actualColumns, List<String> candidates) {
        return candidates.stream().filter(actualColumns::contains).findFirst().orElse(null);
    }
}
