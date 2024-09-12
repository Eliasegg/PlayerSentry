package com.eliaseeg.playersentry.database;

import com.eliaseeg.playersentry.PlayerSentry;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class MutedPlayerManager {

    private final PlayerSentry plugin;

    public MutedPlayerManager(PlayerSentry plugin) {
        this.plugin = plugin;
    }

    public void mutePlayer(UUID uuid, Timestamp expiresAt) {
        new BukkitRunnable() {
            @Override
            public void run() {
                String sql = "INSERT OR REPLACE INTO muted_players (uuid, expires_at) VALUES (?, ?)";
                try (Connection conn = plugin.getSqliteManager().getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, uuid.toString());
                    pstmt.setTimestamp(2, expiresAt);
                    pstmt.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Error muting player", e);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public CompletableFuture<Timestamp> getMuteExpiration(UUID uuid) {
        CompletableFuture<Timestamp> future = new CompletableFuture<>();
        new BukkitRunnable() {
            @Override
            public void run() {
                String sql = "SELECT expires_at FROM muted_players WHERE uuid = ?";
                try (Connection conn = plugin.getSqliteManager().getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, uuid.toString());
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        future.complete(rs.getTimestamp("expires_at"));
                    } else {
                        future.complete(null);
                    }
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Error getting mute expiration for player", e);
                    future.completeExceptionally(e);
                }
            }
        }.runTaskAsynchronously(plugin);
        return future;
    }

    public void unmutePlayer(UUID uuid) {
        new BukkitRunnable() {
            @Override
            public void run() {
                String sql = "DELETE FROM muted_players WHERE uuid = ?";
                try (Connection conn = plugin.getSqliteManager().getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, uuid.toString());
                    pstmt.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Error unmuting player", e);
                }
            }
        }.runTaskAsynchronously(plugin);
    }
}