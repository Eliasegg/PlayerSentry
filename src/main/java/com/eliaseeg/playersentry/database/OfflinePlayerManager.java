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

    public void addOrUpdatePlayer(UUID uuid, String ipAddress, String lastLoggedName) {
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
                    e.printStackTrace();
                    plugin.getLogger().log(Level.SEVERE, "Error adding or updating offline player", e);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public CompletableFuture<String> getIpAddress(UUID uuid) {
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

    public void deletePlayer(UUID uuid) {
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

    public CompletableFuture<Optional<OfflinePlayer>> getOfflinePlayerByName(String name) {
        CompletableFuture<Optional<OfflinePlayer>> future = new CompletableFuture<>();
        new BukkitRunnable() {
            @Override
            public void run() {
                try (Connection conn = plugin.getSqliteManager().getConnection();
                     PreparedStatement pstmt = conn.prepareStatement("SELECT uuid FROM offline_players WHERE lastLoggedName = ?")) {
                    pstmt.setString(1, name);
                    try (ResultSet rs = pstmt.executeQuery()) {
                        if (rs.next()) {
                            UUID uuid = UUID.fromString(rs.getString("uuid"));
                            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
                            future.complete(Optional.of(player));
                        } else {
                            future.complete(Optional.empty());
                        }
                    }
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Error getting offline player by name", e);
                    future.completeExceptionally(e);
                }
            }
        }.runTaskAsynchronously(plugin);
        return future;
    }

    public void updateLastLoggedName(UUID uuid, String lastLoggedName) {
        new BukkitRunnable() {
            @Override
            public void run() {
                String sql = "UPDATE offline_players SET lastLoggedName = ? WHERE uuid = ?";
                try (Connection conn = plugin.getSqliteManager().getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, lastLoggedName);
                    pstmt.setString(2, uuid.toString());
                    pstmt.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Error updating last logged name for player", e);
                }
            }
        }.runTaskAsynchronously(plugin);
    }
}