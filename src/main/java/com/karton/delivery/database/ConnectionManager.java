package com.karton.delivery.database;

import com.karton.delivery.KartonDeliveryAPI;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

public class ConnectionManager {
    public static Connection connection;

    public static void createConnection(String jbdc) {
        try {
            if (ConnectionManager.connection == null) {
                ConnectionManager.connection = DriverManager.getConnection(jbdc);

                KartonDeliveryAPI.LOGGER.info("Connection to database established");
            }
        } catch (SQLException exception) {
            KartonDeliveryAPI.LOGGER.error(exception.getMessage());
        }
    }

    public static void closeConnection() {
        try {
            if (ConnectionManager.connection == null) {
                KartonDeliveryAPI.LOGGER.warn("Connection doesn't been established. Nothing to close");
                return;
            }

            connection.close();
            KartonDeliveryAPI.LOGGER.info("Database connection closed");
        } catch (SQLException e) {
            KartonDeliveryAPI.LOGGER.error(e.getMessage());
        }
    }

    public static final DateTimeFormatter SQLITE_DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
}
