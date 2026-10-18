package com.karton.delivery.config;

public class MainConfig {
    String jdbcConnectionString;

    public MainConfig(String connectionString) {
        this.jdbcConnectionString = connectionString;
    }

    public MainConfig() {
        this("jdbc:sqlite:config/karton-delivery/deliveries.db");
    }

    public String getJdbcConnectionString() {
        return jdbcConnectionString;
    }
}
