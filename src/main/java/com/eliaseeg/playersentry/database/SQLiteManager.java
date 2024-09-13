package com.eliaseeg.playersentry.database;

import com.eliaseeg.playersentry.PlayerSentry;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

public class SQLiteManager {

    private static SQLiteManager instance;
    private final PlayerSentry plugin;
    private Connection connection;

    private SQLiteManager(PlayerSentry plugin) {
        this.plugin = plugin;
    }

    public static synchronized SQLiteManager getInstance(PlayerSentry plugin) {
        if (instance == null) {
            instance = new SQLiteManager(plugin);
        }
        return instance;
    }

    public void initialize() {
        connection = getSQLConnection();
        createTables();
    }

    public synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = getSQLConnection();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error checking connection status", e);
        }
        return connection;
    }

    private Connection getSQLConnection() {
        String dbName = "player_sentry.db";
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder() + "/" + dbName);
            return connection;
        } catch (SQLException | ClassNotFoundException ex) {
            plugin.getLogger().log(Level.SEVERE, "SQLite exception on initialize", ex);
        }
        return null;
    }

    private void createTables() {
        try (Statement statement = connection.createStatement()) {
            // Create offline_players table with lastLoggedName column
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS offline_players (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "uuid VARCHAR(36) NOT NULL," +
                    "ip_address VARCHAR(45) NOT NULL," +
                    "lastLoggedName VARCHAR(16))");

            // Create muted_players table
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS muted_players (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "uuid VARCHAR(36) NOT NULL," +
                    "expires_at TIMESTAMP," +
                    "reason TEXT," +
                    "permanent BOOLEAN)");

            // Create audit_logs table
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS audit_logs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "source VARCHAR(36) NOT NULL," +
                    "target VARCHAR(36) NOT NULL," +
                    "reason TEXT," +
                    "duration VARCHAR(50))");

            plugin.getLogger().info("SQLite tables created successfully.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error creating SQLite tables", e);
        }
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error closing SQLite connection", e);
        }
    }
}