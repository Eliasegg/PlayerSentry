package com.eliaseeg.playersentry.database;

import com.eliaseeg.playersentry.PlayerSentry;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class AuditLogManager {
    private final PlayerSentry plugin;

    public AuditLogManager(PlayerSentry plugin) {
        this.plugin = plugin;
    }

    public void addAuditLog(UUID source, UUID target, String reason, String duration) {
        new BukkitRunnable() {
            @Override
            public void run() {
                String sql = "INSERT INTO audit_logs (source, target, reason, duration) VALUES (?, ?, ?, ?)";
                try (Connection conn = plugin.getSqliteManager().getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, source.toString());
                    pstmt.setString(2, target.toString());
                    pstmt.setString(3, reason);
                    pstmt.setString(4, duration);
                    pstmt.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Error adding audit log", e);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public CompletableFuture<ResultSet> getAuditLogs(UUID target) {
        CompletableFuture<ResultSet> future = new CompletableFuture<>();
        new BukkitRunnable() {
            @Override
            public void run() {
                String sql = "SELECT * FROM audit_logs WHERE target = ?";
                try {
                    Connection conn = plugin.getSqliteManager().getConnection();
                    PreparedStatement pstmt = conn.prepareStatement(sql);
                    pstmt.setString(1, target.toString());
                    ResultSet rs = pstmt.executeQuery();
                    future.complete(rs);
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Error getting audit logs for player", e);
                    future.completeExceptionally(e);
                }
            }
        }.runTaskAsynchronously(plugin);
        return future;
    }
}