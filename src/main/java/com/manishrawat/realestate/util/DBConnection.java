package com.manishrawat.realestate.util;
import com.manishrawat.realestate.config.DBConfig;
import java.sql.*;
public final class DBConnection {
    private DBConnection() { }
    static { try { Class.forName("com.mysql.cj.jdbc.Driver"); } catch (ClassNotFoundException e) { throw new ExceptionInInitializerError(e); } }
    public static Connection getConnection() throws SQLException {
        try { return DriverManager.getConnection(DBConfig.URL, DBConfig.USER, DBConfig.password()); }
        catch (IllegalStateException e) { throw new SQLException("Database credentials are not configured.", e); }
    }
}
