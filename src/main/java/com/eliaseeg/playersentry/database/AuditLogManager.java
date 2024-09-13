package com.eliaseeg.playersentry.database;

import com.eliaseeg.playersentry.PlayerSentry;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;

public class AuditLogManager {
    private final PlayerSentry plugin;

    public AuditLogManager(PlayerSentry plugin) {
        this.plugin = plugin;
    }

    public void addAuditLog(String source, UUID target, String reason, String duration, String punishmentType, boolean isRemoval) {
        new BukkitRunnable() {
            @Override
            public void run() {
                String sql = "INSERT INTO audit_logs (source, target, reason, duration, punishment_type, is_removal) VALUES (?, ?, ?, ?, ?, ?)";
                try (Connection conn = plugin.getSqliteManager().getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, source);
                    pstmt.setString(2, target.toString());
                    pstmt.setString(3, reason);
                    pstmt.setString(4, duration);
                    pstmt.setString(5, punishmentType);
                    pstmt.setBoolean(6, isRemoval);
                    pstmt.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Error adding audit log", e);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    public CompletableFuture<List<AuditLogInfo>> getAuditLogs(UUID target) {
        CompletableFuture<List<AuditLogInfo>> future = new CompletableFuture<>();
        new BukkitRunnable() {
            @Override
            public void run() {
                String sql = "SELECT * FROM audit_logs WHERE target = ? ORDER BY id DESC";
                List<AuditLogInfo> auditLogs = new ArrayList<>();
                try (Connection conn = plugin.getSqliteManager().getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, target.toString());
                    ResultSet rs = pstmt.executeQuery();
                    while (rs.next()) {
                        AuditLogInfo logInfo = new AuditLogInfo(
                            rs.getString("source"),
                            UUID.fromString(rs.getString("target")),
                            rs.getString("reason"),
                            rs.getString("duration"),
                            rs.getString("punishment_type"),
                            rs.getBoolean("is_removal")
                        );
                        auditLogs.add(logInfo);
                    }
                    future.complete(auditLogs);
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Error getting audit logs for player", e);
                    future.completeExceptionally(e);
                }
            }
        }.runTaskAsynchronously(plugin);
        return future;
    }

    public static class AuditLogInfo {
        private final String source;
        private final UUID target;
        private final String reason;
        private final String duration;
        private final String punishmentType;
        private final boolean isRemoval;

        public AuditLogInfo(String source, UUID target, String reason, String duration, String punishmentType, boolean isRemoval) {
            this.source = source;
            this.target = target;
            this.reason = reason;
            this.duration = duration;
            this.punishmentType = punishmentType;
            this.isRemoval = isRemoval;
        }

        public String getSource() { return source; }
        public UUID getTarget() { return target; }
        public String getReason() { return reason; }
        public String getDuration() { return duration; }
        public String getPunishmentType() { return punishmentType; }
        public boolean isRemoval() { return isRemoval; }
    }
}