package com.eliaseeg.playersentry.database;

import com.eliaseeg.playersentry.PlayerSentry;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class OfflinePlayerManager {

    private final PlayerSentry plugin;

    public OfflinePlayerManager(PlayerSentry plugin) {
        this.plugin = plugin;
    }

    /**
     * Add or update an offline player's information in the database.
     * @param uuid The UUID of the player.
     * @param ipAddress The IP address of the player.
     * @param lastLoggedName The last known username of the player.
     */
    public void addOrUpdatePlayer(UUID uuid, String ipAddress, String lastLoggedName) {
        if (uuid == null || ipAddress == null || lastLoggedName == null) {
            plugin.getLogger().warning("Invalid parameters for addOrUpdatePlayer");
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                try (Connection conn = plugin.getSqliteManager().getConnection();
                     PreparedStatement pstmt = conn.prepareStatement("INSERT OR REPLACE INTO offline_players (uuid, ip_address, lastLoggedName) VALUES (?, ?, ?)")) {
                    pstmt.setString(1, uuid.toString());
                    pstmt.setString(2, ipAddress);
                    pstmt.setString(3, lastLoggedName);
                    pstmt.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Error adding or updating offline player", e);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Get the IP address of an offline player.
     * @param uuid The UUID of the player.
     * @return A CompletableFuture that resolves to the player's IP address, or null if not found.
     */
    public CompletableFuture<String> getIpAddress(UUID uuid) {
        if (uuid == null) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<String> future = new CompletableFuture<>();
        new BukkitRunnable() {
            @Override
            public void run() {
                String sql = "SELECT ip_address FROM offline_players WHERE uuid = ?";
                try (Connection conn = plugin.getSqliteManager().getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, uuid.toString());
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            future.complete(rs.getString("ip_address"));
                        } else {
                            future.complete(null);
                        }
                    }
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Error getting IP address for player", e);
                    future.completeExceptionally(e);
                }
            }
        }.runTaskAsynchronously(plugin);
        return future;
    }

    /**
     * Delete an offline player's information from the database.
     * @param uuid The UUID of the player to delete.
     */
    public void deletePlayer(UUID uuid) {
        if (uuid == null) {
            plugin.getLogger().warning("Invalid UUID for deletePlayer");
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                String sql = "DELETE FROM offline_players WHERE uuid = ?";
                try (Connection conn = plugin.getSqliteManager().getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, uuid.toString());
                    pstmt.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Error deleting offline player", e);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Get an OfflinePlayer by their last known username.
     * @param name The last known username of the player.
     * @return A CompletableFuture that resolves to an Optional containing the OfflinePlayer if found, or empty if not.
     */
    public CompletableFuture<Optional<OfflinePlayer>> getOfflinePlayerByName(String name) {
        if (name == null || name.isEmpty()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = plugin.getSqliteManager().getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("SELECT uuid FROM offline_players WHERE LOWER(lastLoggedName) = LOWER(?)")) {
                pstmt.setString(1, name);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        UUID uuid = UUID.fromString(rs.getString("uuid"));
                        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                        return Optional.of(player);
                    } else {
                        return Optional.empty();
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error getting offline player by name", e);
                throw new RuntimeException(e);
            }
        });
    }


}