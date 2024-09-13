package com.eliaseeg.playersentry.database;

import com.eliaseeg.playersentry.PlayerSentry;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.*;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.Optional;
import java.util.logging.Level;

public class MutedPlayerManager {

    private final PlayerSentry plugin;

    public MutedPlayerManager(PlayerSentry plugin) {
        this.plugin = plugin;
    }

    /**
     * Mute a player for a specified duration.
     * @param uuid The UUID of the player to mute.
     * @param expiresAt The date when the mute expires.
     * @param reason The reason for the mute.
     * @param permanent Whether the mute is permanent.
     */
    public void mutePlayer(UUID uuid, Date expiresAt, String reason, boolean permanent) {
        if (uuid == null || expiresAt == null || reason == null) {
            plugin.getLogger().warning("Invalid parameters for mutePlayer");
            return;
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                String sql = "INSERT OR REPLACE INTO muted_players (uuid, expires_at, reason, permanent) VALUES (?, ?, ?, ?)";
                try (Connection conn = plugin.getSqliteManager().getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, uuid.toString());
                    pstmt.setLong(2, expiresAt.getTime());
                    pstmt.setString(3, reason);
                    pstmt.setBoolean(4, permanent);
                    pstmt.executeUpdate();
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Error muting player", e);
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Unmute a player.
     * @param uuid The UUID of the player to unmute.
     */
    public void unmutePlayer(UUID uuid) {
        if (uuid == null) {
            plugin.getLogger().warning("Invalid UUID for unmutePlayer");
            return;
        }

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

    /**
     * Get mute information for a player.
     * @param uuid The UUID of the player.
     * @return A CompletableFuture that resolves to an Optional containing MuteInfo if the player is muted, or empty if not.
     */
    public CompletableFuture<Optional<MuteInfo>> getMuteInfo(UUID uuid) {
        if (uuid == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        CompletableFuture<Optional<MuteInfo>> future = new CompletableFuture<>();
        new BukkitRunnable() {
            @Override
            public void run() {
                String sql = "SELECT expires_at, reason, permanent FROM muted_players WHERE uuid = ?";
                try (Connection conn = plugin.getSqliteManager().getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, uuid.toString());
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        long expiresAt = rs.getLong("expires_at");
                        String reason = rs.getString("reason");
                        boolean permanent = rs.getBoolean("permanent");
                        MuteInfo muteInfo = new MuteInfo(new Date(expiresAt), reason, permanent);
                        future.complete(Optional.of(muteInfo));
                    } else {
                        future.complete(Optional.empty());
                    }
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Error getting mute info for player", e);
                    future.completeExceptionally(e);
                }
            }
        }.runTaskAsynchronously(plugin);
        return future;
    }

    /**
     * Check if a player is currently muted.
     * @param uuid The UUID of the player to check.
     * @return A CompletableFuture that resolves to true if the player is muted, false otherwise.
     */
    public CompletableFuture<Boolean> isPlayerMuted(UUID uuid) {
        if (uuid == null) {
            return CompletableFuture.completedFuture(false);
        }

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        new BukkitRunnable() {
            @Override
            public void run() {
                String sql = "SELECT expires_at FROM muted_players WHERE uuid = ?";
                try (Connection conn = plugin.getSqliteManager().getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, uuid.toString());
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        long expiresAt = rs.getLong("expires_at");
                        boolean isMuted = expiresAt > System.currentTimeMillis();
                        future.complete(isMuted);
                    } else {
                        future.complete(false);
                    }
                } catch (SQLException e) {
                    plugin.getLogger().log(Level.SEVERE, "Error checking if player is muted", e);
                    future.completeExceptionally(e);
                }
            }
        }.runTaskAsynchronously(plugin);
        return future;
    }

    private CompletableFuture<Date> getMuteExpiration(UUID uuid) {
        CompletableFuture<Date> future = new CompletableFuture<>();
        new BukkitRunnable() {
            @Override
            public void run() {
                String sql = "SELECT expires_at FROM muted_players WHERE uuid = ?";
                try (Connection conn = plugin.getSqliteManager().getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, uuid.toString());
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        long expiresAt = rs.getLong("expires_at");
                        future.complete(new Date(expiresAt));
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

    public static class MuteInfo {
        private final Date expiresAt;
        private final String reason;
        private final boolean permanent;

        public MuteInfo(Date expiresAt, String reason, boolean permanent) {
            this.expiresAt = expiresAt;
            this.reason = reason;
            this.permanent = permanent;
        }

        public Date getExpiresAt() { return expiresAt; }
        public String getReason() { return reason; }
        public boolean isPermanent() { return permanent; }
    }

}