package com.manishrawat.realestate.dao;

import com.manishrawat.realestate.model.Property;
import com.manishrawat.realestate.util.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class PropertyDAO implements GenericRepository<Property> {
    private static final String PROPERTY_COLUMNS = "SELECT column_name FROM information_schema.columns "
            + "WHERE table_schema = DATABASE() AND table_name = ? AND column_name IN (?, ?)";

    public int save(Property property) throws Exception {
        try (Connection connection = DBConnection.getConnection()) {
            List<String> rentColumns = rentColumns(connection);
            List<String> columns = new ArrayList<>(List.of("manager_id", "title", "address", "city"));
            columns.addAll(rentColumns);
            columns.addAll(List.of("property_type", "bedrooms", "description", "status"));

            String placeholders = String.join(",", java.util.Collections.nCopies(columns.size(), "?"));
            String sql = "INSERT INTO properties (" + String.join(",", columns) + ") VALUES (" + placeholders + ")";
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                int index = bindListing(statement, property, rentColumns, false);
                statement.setString(index++, "PENDING");
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    return keys.next() ? keys.getInt(1) : 0;
                }
            }
        }
    }

    public void setStatus(int id, String status) throws Exception {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("UPDATE properties SET status=? WHERE id=?")) {
            statement.setString(1, status);
            statement.setInt(2, id);
            if (statement.executeUpdate() == 0) throw new IllegalArgumentException("Property not found.");
        }
    }

    public void update(Property property, int managerId) throws Exception {
        try (Connection connection = DBConnection.getConnection()) {
            List<String> rentColumns = rentColumns(connection);
            StringBuilder sql = new StringBuilder("UPDATE properties SET title=?,address=?,city=?");
            for (String column : rentColumns) sql.append(',').append(column).append("=?");
            sql.append(",property_type=?,bedrooms=?,description=?,status='PENDING' WHERE id=? AND manager_id=?");

            try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
                int index = bindListing(statement, property, rentColumns, true);
                statement.setInt(index++, property.getId());
                statement.setInt(index, managerId);
                if (statement.executeUpdate() == 0) {
                    throw new IllegalArgumentException("Listing not found or access denied.");
                }
            }
        }
    }

    public void delete(int id, int managerId) throws Exception {
        String sql = "DELETE FROM properties WHERE id=? AND manager_id=? "
                + "AND NOT EXISTS (SELECT 1 FROM rental_applications WHERE property_id=?)";
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setInt(2, managerId);
            statement.setInt(3, id);
            if (statement.executeUpdate() == 0) {
                throw new IllegalArgumentException("Listing not found, access denied, or rental history exists. Keep the listing to preserve that history.");
            }
        }
    }

    public List<Property> findAll() throws Exception {
        return query("SELECT * FROM properties ORDER BY id DESC");
    }

    public List<Property> approved(String city, String type) throws Exception {
        String sql = "SELECT * FROM properties WHERE status='APPROVED' AND (?='' OR city LIKE ?) "
                + "AND (?='' OR property_type=?) ORDER BY id DESC";
        try (Connection connection = DBConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, city);
            statement.setString(2, "%" + city + "%");
            statement.setString(3, type);
            statement.setString(4, type);
            try (ResultSet result = statement.executeQuery()) {
                return mapList(result);
            }
        }
    }

    public Property findById(int id) throws Exception {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM properties WHERE id=?")) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                List<Property> properties = mapList(result);
                return properties.isEmpty() ? null : properties.get(0);
            }
        }
    }

    public List<Property> findByManager(int id) throws Exception {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM properties WHERE manager_id=? ORDER BY id DESC")) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return mapList(result);
            }
        }
    }

    public int countByStatus(String status) throws Exception {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM properties WHERE status=?")) {
            statement.setString(1, status);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private List<String> rentColumns(Connection connection) throws SQLException {
        Set<String> found = new HashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(PROPERTY_COLUMNS)) {
            statement.setString(1, "properties");
            statement.setString(2, "rent");
            statement.setString(3, "price");
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) found.add(result.getString(1).toLowerCase(Locale.ROOT));
            }
        }
        List<String> ordered = new ArrayList<>(2);
        if (found.contains("rent")) ordered.add("rent");
        if (found.contains("price")) ordered.add("price");
        if (ordered.isEmpty()) throw new SQLException("The properties table must contain a rent or legacy price column.");
        return ordered;
    }

    private int bindListing(PreparedStatement statement, Property property, List<String> rentColumns, boolean updating)
            throws SQLException {
        int index = 1;
        if (!updating) statement.setInt(index++, property.getManagerId());
        statement.setString(index++, property.getTitle());
        statement.setString(index++, property.getAddress());
        statement.setString(index++, property.getCity());
        for (String ignored : rentColumns) statement.setBigDecimal(index++, property.getRent());
        statement.setString(index++, property.getPropertyType());
        statement.setInt(index++, property.getBedrooms());
        statement.setString(index++, property.getDescription());
        return index;
    }

    private List<Property> query(String sql) throws Exception {
        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            return mapList(result);
        }
    }

    private List<Property> mapList(ResultSet result) throws Exception {
        List<Property> properties = new ArrayList<>();
        ResultSetMetaData metadata = result.getMetaData();
        Set<String> columns = new HashSet<>();
        for (int i = 1; i <= metadata.getColumnCount(); i++) {
            columns.add(metadata.getColumnLabel(i).toLowerCase(Locale.ROOT));
        }
        String rentColumn = columns.contains("rent") ? "rent" : "price";

        while (result.next()) {
            Property property = new Property();
            property.setId(result.getInt("id"));
            property.setManagerId(result.getInt("manager_id"));
            property.setTitle(result.getString("title"));
            property.setAddress(result.getString("address"));
            property.setCity(result.getString("city"));
            property.setRent(result.getBigDecimal(rentColumn));
            property.setPropertyType(result.getString("property_type"));
            property.setBedrooms(result.getInt("bedrooms"));
            property.setDescription(result.getString("description"));
            property.setStatus(result.getString("status"));
            properties.add(property);
        }
        return properties;
    }
}
