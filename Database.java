package com.atm;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Database {
    private static final String URL = "jdbc:mysql://localhost:3306/atm_management";
    private static final String USER = "root";
    private static final String PASSWORD =
            System.getenv().getOrDefault("ATM_DB_PASSWORD", "Joeljude263_*");

    private Database() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
