package com.eliaseeg.playersentry.utils;

import com.eliaseeg.playersentry.PlayerSentry;
import com.eliaseeg.playersentry.database.AuditLogManager;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.ban.IpBanList;
import org.bukkit.ban.ProfileBanList;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class PlayerUtils {

    public static void getAuditLogs(CommandSender sender, String playerName, int page) {
        getPlayer(playerName).thenAccept(optionalPlayer -> {
            if (!optionalPlayer.isPresent()) {
                MessageUtils.buildMessage(sender, "&cPlayer &7not found | Ensure that the player has played on the server before.");
                return;
            }
            OfflinePlayer player = optionalPlayer.get();
            PlayerSentry.getInstance().getAuditLogManager().getAuditLogs(player.getUniqueId())
                    .thenAccept(logs -> displayAuditLogs(sender, player, logs, page))
                    .exceptionally(ex -> {
                        PlayerSentry.getInstance().getLogger().log(Level.SEVERE, "Error getting audit logs for player: " + playerName, ex);
                        MessageUtils.buildMessage(sender, "&cAn error occurred while retrieving audit logs.");
                        return null;
                    });
        }).exceptionally(ex -> {
            PlayerSentry.getInstance().getLogger().log(Level.SEVERE, "Error retrieving player: " + playerName, ex);
            MessageUtils.buildMessage(sender, "&cAn error occurred while looking up the player.");
            return null;
        });
    }

    private static void displayAuditLogs(CommandSender sender, OfflinePlayer player, List<AuditLogManager.AuditLogInfo> logs, int page) {
        int LOGS_PER_PAGE = PlayerSentry.getInstance().getConfig().getInt("LOGS_PER_PAGE");
        if (logs.isEmpty()) {
            MessageUtils.buildMessage(sender, "&6Audit logs for &f" + player.getName() + "&6: This player hasn't received any punishment.");
            return;
        }

        int totalPages = (logs.size() + LOGS_PER_PAGE - 1) / LOGS_PER_PAGE;
        page = Math.max(1, Math.min(page, totalPages));

        int startIndex = (page - 1) * LOGS_PER_PAGE;
        int endIndex = Math.min(startIndex + LOGS_PER_PAGE, logs.size());

        MessageUtils.buildMessage(sender, "&6Audit logs for &f" + player.getName() + " &6(Page " + page + "/" + totalPages + "):");
        for (int i = startIndex; i < endIndex; i++) {
            AuditLogManager.AuditLogInfo log = logs.get(i);
            String message = String.format("&7[%d] &f%s &7by &f%s&7: &f%s &7(%s)",
                    i + 1, log.getPunishmentType(), log.getSource(), log.getReason(), log.getDuration());
            MessageUtils.buildMessage(sender, message);
        }

        if (page < totalPages) {
            MessageUtils.buildMessage(sender, "&6Use &f/slogs " + player.getName() + " " + (page + 1) + " &6to see the next page.");
        }
    }

    /**
     * Mute a player for a specified duration.
     * If the duration is null, the mute is permanent.
     * @param sender The command sender that executed the mute.
     * @param playerName The name of the player.
     * @param reason The reason for the mute.
     * @param duration The duration of the mute.
     *
     * @return A CompletableFuture that completes with true if the mute was successful, false otherwise.
     */
    public static CompletableFuture<Boolean> mutePlayer(CommandSender sender, String playerName, String reason, Date duration) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();

        getPlayer(playerName).thenAccept(optionalPlayer -> {
            if (!optionalPlayer.isPresent()) {
                MessageUtils.buildMessage(sender, "&cPlayer &7not found | Ensure that the player has played on the server before.");
                result.complete(false);
                return;
            }

            OfflinePlayer player = optionalPlayer.get();

            PlayerSentry.getInstance().getMutedPlayerManager().isPlayerMuted(player.getUniqueId()).thenAccept(isMuted -> {
                if (isMuted) {
                    MessageUtils.buildMessage(sender, "&cPlayer &7" + playerName + " &cis already muted.");
                    result.complete(false);
                    return;
                }

                boolean permanent = duration == null;
                Date muteExpiration = permanent ? new Date(Long.MAX_VALUE) : duration;
                String durationMessage = permanent ? "permanently" : "until &a" + muteExpiration;

                String banMessage = ChatColor.translateAlternateColorCodes('&', "&c" + reason);

                PlayerSentry.getInstance().getMutedPlayerManager().mutePlayer(player.getUniqueId(), muteExpiration, reason, permanent);
                MessageUtils.buildMessage(sender, "Muted &a" + playerName + " &7" + durationMessage);

                if (player.isOnline()) {
                    Player onlinePlayer = (Player) player;
                    MessageUtils.buildMessage(onlinePlayer, "&cYou have been muted " + durationMessage + "&c. Reason: &7" + banMessage);
                }

                result.complete(true);
            }).exceptionally(ex -> {
                PlayerSentry.getInstance().getLogger().log(Level.SEVERE, "Error while muting player: " + playerName, ex);
                MessageUtils.buildMessage(sender, "&cAn error occurred while muting the player. Please try again.");
                result.complete(false);
                return null;
            });
        }).exceptionally(ex -> {
            MessageUtils.buildMessage(sender, "&cAn error occurred while looking up the player.");
            result.complete(false);
            return null;
        });

        return result;
    }

    /**
     * Unmute a player.
     * @param sender The command sender that executed unmute.
     * @param playerName The name of the player.
     *
     * @return A CompletableFuture that completes with true if unmute was successful, false otherwise.
     */
    public static CompletableFuture<Boolean> unmutePlayer(CommandSender sender, String playerName) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();

        getPlayer(playerName).thenAccept(optionalPlayer -> {
            if (!optionalPlayer.isPresent()) {
                MessageUtils.buildMessage(sender, "&cPlayer &7not found | Ensure that the player has played on the server before.");
                result.complete(false);
                return;
            }

            OfflinePlayer player = optionalPlayer.get();

            PlayerSentry.getInstance().getMutedPlayerManager().isPlayerMuted(player.getUniqueId()).thenAccept(isMuted -> {
                if (!isMuted) {
                    MessageUtils.buildMessage(sender, "&cPlayer &7" + playerName + " &cis not muted.");
                    result.complete(false);
                    return;
                }

                PlayerSentry.getInstance().getMutedPlayerManager().unmutePlayer(player.getUniqueId());
                MessageUtils.buildMessage(sender, "Unmuted &a" + playerName);

                if (player.isOnline()) {
                    Player onlinePlayer = (Player) player;
                    MessageUtils.buildMessage(onlinePlayer, "&aYou have been unmuted.");
                }

                result.complete(true);
            }).exceptionally(ex -> {
                PlayerSentry.getInstance().getLogger().log(Level.SEVERE, "Error while unmuting player: " + playerName, ex);
                MessageUtils.buildMessage(sender, "&cAn error occurred while unmuting the player. Please try again.");
                result.complete(false);
                return null;
            });
        }).exceptionally(ex -> {
            MessageUtils.buildMessage(sender, "&cAn error occurred while looking up the player.");
            result.complete(false);
            return null;
        });

        return result;
    }

    /**
     * Ban a player IP permanently from the server.
     * @param sender The command sender that executed the ban.
     * @param playerName The name of the player.
     * @param reason The reason for the ban.
     *
     * @return A CompletableFuture that completes with true if the ban was successful, false otherwise.
     */
    public static CompletableFuture<Boolean> banPlayerIP(CommandSender sender, String playerName, String reason) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();

        getIPFromPlayer(playerName).thenAccept(ip -> {
            if (ip == null) {
                MessageUtils.buildMessage(sender, "&cCould not retrieve IP address for player. They may not have joined the server before.");
                result.complete(false);
                return;
            }

            String banMessage = ChatColor.translateAlternateColorCodes('&', "&c" + reason);

            IpBanList ipBanList = Bukkit.getBanList(BanList.Type.IP);
            ipBanList.addBan(ip, banMessage, (Date) null, sender.getName());

            Player player = Bukkit.getPlayer(playerName);
            if (player != null && player.isOnline()) {
                PlayerSentry.getInstance().getOfflinePlayerManager().addOrUpdatePlayer(player.getUniqueId(), player.getAddress().getAddress().getHostAddress(), player.getName());
                player.kickPlayer(banMessage);
            }

            MessageUtils.buildMessage(sender, "Blacklisted &a" + playerName);
            result.complete(true);
        }).exceptionally(ex -> {
            PlayerSentry.getInstance().getLogger().log(Level.SEVERE, "Error banning player IP: " + playerName, ex);
            MessageUtils.buildMessage(sender, "&cAn error occurred while banning player IP. Please check the server logs.");
            result.complete(false);
            return null;
        });

        return result;
    }

    /**
     * Unban a player IP permanently from the server.
     * @param sender The command sender that executed the unban.
     * @param playerName The name of the player.
     *
     * @return A CompletableFuture that completes with true if unban was successful, false otherwise.
     */
    public static CompletableFuture<Boolean> unbanPlayerIP(CommandSender sender, String playerName) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();

        getIPFromPlayer(playerName).thenAccept(ip -> {
            if (ip == null) {
                MessageUtils.buildMessage(sender, "&cCould not retrieve IP address for player.");
                result.complete(false);
                return;
            }

            IpBanList ipBanList = Bukkit.getBanList(BanList.Type.IP);
            if (!ipBanList.isBanned(ip)) {
                MessageUtils.buildMessage(sender, "&cIP address of &7" + playerName + " &cis not blacklisted.");
                result.complete(false);
                return;
            }

            ipBanList.pardon(ip);
            MessageUtils.buildMessage(sender, "Unblacklisted IP of &a" + playerName);
            result.complete(true);
        }).exceptionally(ex -> {
            PlayerSentry.getInstance().getLogger().log(Level.SEVERE, "Error unbanning player IP: " + playerName, ex);
            MessageUtils.buildMessage(sender, "&cAn error occurred while unbanning player IP.");
            result.complete(false);
            return null;
        });

        return result;
    }

    /**
     * Ban a player for a fixed duration.
     * @param sender The command sender that executed the ban.
     * @param playerName The name of the player.
     * @param reason The reason for the ban.
     * @param duration The duration of the ban.
     *
     * @return A CompletableFuture that completes with true if ban was successful, false otherwise.
     */
    public static CompletableFuture<Boolean> banPlayer(CommandSender sender, String playerName, String reason, Date duration) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();

        getPlayer(playerName).thenAccept(optionalPlayer -> {
            if (!optionalPlayer.isPresent()) {
                MessageUtils.buildMessage(sender, "&cPlayer &7not found | Ensure that the player has played on the server before.");
                result.complete(false);
                return;
            }

            OfflinePlayer player = optionalPlayer.get();

            if (player.isBanned()) {
                MessageUtils.buildMessage(sender, "&cPlayer &7is already banned.");
                result.complete(false);
                return;
            }

            String banMessage = ChatColor.translateAlternateColorCodes('&', "&c" + reason);

            ProfileBanList profileBanList = Bukkit.getBanList(BanList.Type.PROFILE);
            profileBanList.addBan(player.getPlayerProfile(), banMessage, duration, sender.getName());

            if (player.isOnline()) {
                Player onlinePlayer = player.getPlayer();
                PlayerSentry.getInstance().getOfflinePlayerManager().addOrUpdatePlayer(onlinePlayer.getUniqueId(), onlinePlayer.getAddress().getAddress().getHostAddress(), onlinePlayer.getName());
                ((Player) player).kickPlayer(banMessage);
            }

            MessageUtils.buildMessage(sender, "Banned &a" + playerName);
            result.complete(true);
        }).exceptionally(ex -> {
            MessageUtils.buildMessage(sender, "&cAn error occurred while looking up the player.");
            result.complete(false);
            return null;
        });

        return result;
    }

    /**
     * Unban a player.
     * @param sender The command sender that executed the unban.
     * @param playerName The name of the player.
     *
     * @return A CompletableFuture that completes with true if unban was successful, false otherwise.
     */
    public static CompletableFuture<Boolean> unbanPlayer(CommandSender sender, String playerName) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();

        getPlayer(playerName).thenAccept(optionalPlayer -> {
            if (!optionalPlayer.isPresent()) {
                MessageUtils.buildMessage(sender, "&cPlayer &7not found.");
                result.complete(false);
                return;
            }

            OfflinePlayer player = optionalPlayer.get();

            if (!player.isBanned()) {
                MessageUtils.buildMessage(sender, "&cPlayer &7is not banned.");
                result.complete(false);
                return;
            }

            ProfileBanList profileBanList = Bukkit.getBanList(BanList.Type.PROFILE);
            profileBanList.pardon(player.getPlayerProfile());
            MessageUtils.buildMessage(sender, "Unbanned &a" + playerName);
            result.complete(true);
        }).exceptionally(ex -> {
            MessageUtils.buildMessage(sender, "&cAn error occurred while looking up the player.");
            result.complete(false);
            return null;
        });

        return result;
    }

    /**
     * Get a player by name, whether online or offline.
     * An offline player counts if the player has played on the server before. They are added to the database.
     * @param playerName The name of the player.
     * @return A CompletableFuture that resolves to an Optional containing the OfflinePlayer if found, or empty if not.
     */
    public static CompletableFuture<Optional<OfflinePlayer>> getPlayer(String playerName) {
        CompletableFuture<Optional<OfflinePlayer>> future = new CompletableFuture<>();

        // First, try to find the Player object in the server. That means the player is online.
        Optional<OfflinePlayer> playerOptional = Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.getName().equalsIgnoreCase(playerName))
                .map(player -> (OfflinePlayer) player)
                .findFirst();
        if (playerOptional.isPresent()) {
            future.complete(playerOptional);
            return future;
        }

        // If still not found, try looking up the player by their last known name in the database.
        PlayerSentry.getInstance().getOfflinePlayerManager().getOfflinePlayerByName(playerName)
                .thenAccept(future::complete)
                .exceptionally(ex -> {
                    future.completeExceptionally(ex);
                    return null;
                });

        return future;
    }

    /**
     * Get the IP address of a player, whether online or offline.
     * If the player is online, the IP address is retrieved from the player's object.
     * If the player is offline, the IP address is retrieved from the database.
     *
     * @param playerName The name of the player to get the IP address of.
     * @return A CompletableFuture that completes with the IP address of the player or null if not found.
     */
    public static CompletableFuture<InetAddress> getIPFromPlayer(String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        if (player != null) {
            return CompletableFuture.completedFuture(player.getAddress().getAddress());
        }

        return PlayerSentry.getInstance().getOfflinePlayerManager().getOfflinePlayerByName(playerName)
                .thenCompose(optionalPlayer -> {
                    if (optionalPlayer.isPresent()) {
                        return PlayerSentry.getInstance().getOfflinePlayerManager()
                                .getIpAddress(optionalPlayer.get().getUniqueId())
                                .thenApply(ipAddress -> {
                                    if (ipAddress != null) {
                                        try {
                                            return InetAddress.getByName(ipAddress);
                                        } catch (UnknownHostException e) {
                                            PlayerSentry.getInstance().getLogger().log(Level.SEVERE, "Error getting IP address for player", e);
                                        }
                                    }
                                    return null;
                                });
                    }
                    return CompletableFuture.completedFuture(null);
                });
    }

}
