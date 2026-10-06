package com.manishrawat.realestate.config;

/** Database settings are read at runtime so Tomcat can supply credentials securely. */
public final class DBConfig {
    private DBConfig() { }
    public static final String URL = System.getenv().getOrDefault("REAL_ESTATE_DB_URL", "jdbc:mysql://localhost:3306/real_estate_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&connectTimeout=5000&socketTimeout=10000");
    public static final String USER = System.getenv().getOrDefault("REAL_ESTATE_DB_USER", "root");
    public static String password() {
        String password = System.getenv("REAL_ESTATE_DB_PASSWORD");
        if (password == null || password.isBlank()) throw new IllegalStateException("Set REAL_ESTATE_DB_PASSWORD in the Tomcat environment.");
        return password;
    }
}
