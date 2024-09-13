package com.eliaseeg.playersentry.utils;

import com.eliaseeg.playersentry.PlayerSentry;
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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.logging.Level;

public class PlayerUtils {

    /**
     * Mute a player for a specified duration.
     * If the duration is null, the mute is permanent.
     * @param sender The command sender that executed the mute.
     * @param playerName The name of the player.
     * @param reason The reason for the mute.
     * @param duration The duration of the mute.
     */
    public static void mutePlayer(CommandSender sender, String playerName, String reason, Date duration) {
        boolean permanent = duration == null;
        Date muteExpiration = permanent ? new Date(Long.MAX_VALUE) : duration;
        String durationMessage = permanent ? "permanently" : "until &a" + muteExpiration;

        boolean playerExists = getPlayer(playerName, player -> {
            PlayerSentry.getInstance().getMutedPlayerManager().isPlayerMuted(player.getUniqueId()).thenAccept(isMuted -> {
                if (isMuted) {
                    MessageUtils.buildMessage(sender, "&cPlayer &7" + playerName + " &cis already muted.");
                    return;
                }

                String banMessage = ChatColor.translateAlternateColorCodes('&', "&c" + reason);

                PlayerSentry.getInstance().getMutedPlayerManager().mutePlayer(player.getUniqueId(), muteExpiration, reason, permanent);
                MessageUtils.buildMessage(sender, "Muted &a" + playerName + " &7" + durationMessage);
                
                // If the player is online, notify them
                Player onlinePlayer = player.getPlayer();
                if (onlinePlayer != null && onlinePlayer.isOnline()) {
                    MessageUtils.buildMessage(onlinePlayer, "&cYou have been muted " + durationMessage + "&c. Reason: &7" + banMessage);
                }

            });
        });

        if (!playerExists) {
            MessageUtils.buildMessage(sender, "&cPlayer &7not found | Ensure that the player has played on the server before.");
        }
    }

    /**
     * Unmute a player.
     * @param sender The command sender that executed the unmute.
     * @param playerName The name of the player.
     */
    public static void unmutePlayer(CommandSender sender, String playerName) {
        boolean playerExists = getPlayer(playerName, player -> {
            PlayerSentry.getInstance().getMutedPlayerManager().isPlayerMuted(player.getUniqueId()).thenAccept(isMuted -> {
                if (isMuted) {
                    PlayerSentry.getInstance().getMutedPlayerManager().unmutePlayer(player.getUniqueId());
                    MessageUtils.buildMessage(sender, "Unmuted &a" + playerName);
                    
                    // If the player is online, notify them
                    Player onlinePlayer = player.getPlayer();
                    if (onlinePlayer != null && onlinePlayer.isOnline()) {
                        MessageUtils.buildMessage(onlinePlayer, "&aYou have been unmuted.");
                    }
                } else {
                    MessageUtils.buildMessage(sender, "&cPlayer &7" + playerName + " &cis not muted.");
                }
            });
        });

        if (!playerExists) {
            MessageUtils.buildMessage(sender, "&cPlayer &7not found | Ensure that the player has played on the server before.");
        }
    }

    /**
     * Ban a player IP permanently from the server.
     * @param sender The command sender that executed the ban.
     * @param playerName The name of the player.
     * @param reason The reason for the ban.
     */
    public static void banPlayerIP(CommandSender sender, String playerName, String reason) {
        CompletableFuture<InetAddress> ipFuture = PlayerUtils.getIPFromPlayer(playerName);
        ipFuture.thenAccept(ip -> {
            if (ip == null) {
                MessageUtils.buildMessage(sender, "&cCould not retrieve IP address for player. They may not have joined the server before.");
                return;
            }

            String banMessage = ChatColor.translateAlternateColorCodes('&', "&c" + reason);

            IpBanList ipBanList = Bukkit.getBanList(BanList.Type.IP);
            ipBanList.addBan(ip, banMessage, (Date)null, sender.getName());

            Player player = Bukkit.getPlayer(playerName);
            if (player != null && player.isOnline()) {
                player.kickPlayer(banMessage);
            }

            MessageUtils.buildMessage(sender, "Blacklisted &a" + playerName);
        }).exceptionally(e -> {
            PlayerSentry.getInstance().getLogger().log(Level.SEVERE, "Error banning player IP: " + playerName, e);
            MessageUtils.buildMessage(sender, "&cAn error occurred while banning player IP. Please check the server logs.");
            return null;
        });
    }

    /**
     * Unban a player IP permanently from the server.
     * @param sender The command sender that executed the unban.
     * @param playerName The name of the player.
     */
    public static void unbanPlayerIP(CommandSender sender, String playerName) {
        CompletableFuture<InetAddress> ipFuture = PlayerUtils.getIPFromPlayer(playerName);
        ipFuture.thenAccept(ip -> {
            if (ip == null) {
                MessageUtils.buildMessage(sender, "&cCould not retrieve IP address for player.");
                return;
            }

            IpBanList ipBanList = Bukkit.getBanList(BanList.Type.IP);
            if (!ipBanList.isBanned(ip)) {
                MessageUtils.buildMessage(sender, "&cIP address of &7" + playerName + " &cis not blacklisted.");
                return;
            }

            ipBanList.pardon(ip);
            MessageUtils.buildMessage(sender, "Unblacklisted IP of &a" + playerName);
        }).exceptionally(e -> {
            PlayerSentry.getInstance().getLogger().log(Level.SEVERE, "Error unbanning player IP: " + playerName, e);
            MessageUtils.buildMessage(sender, "&cAn error occurred while unbanning player IP.");
            return null;
        });
    }

    /**
     * Ban a player for a fixed duration.
     * @param sender The command sender that executed the ban.
     * @param playerName The name of the player.
     * @param reason The reason for the ban.
     * @param duration The duration of the ban.
     */
    public static void banPlayer(CommandSender sender, String playerName, String reason, Date duration) {
        boolean playerExists = PlayerUtils.getPlayer(playerName, player -> {
            if (player.isBanned()) {
                MessageUtils.buildMessage(sender, "&cPlayer &7is already banned.");
                return;
            }

            String banMessage = ChatColor.translateAlternateColorCodes('&', "&c" + reason);

            ProfileBanList profileBanList = Bukkit.getBanList(BanList.Type.PROFILE);
            profileBanList.addBan(player.getPlayerProfile(), banMessage, duration, sender.getName());
            if (player.isOnline()) {
                ((Player)player).kickPlayer(banMessage);
            }

            MessageUtils.buildMessage(sender, "Banned &a" + playerName);
        });

        if (!playerExists) {
            MessageUtils.buildMessage(sender, "&cPlayer &7not found | Ensure that the player has played on the server before.");
        }
    }

    /**
     * Unban a player.
     * @param sender The command sender that executed the unban.
     * @param playerName The name of the player.
     */
    public static void unbanPlayer(CommandSender sender, String playerName) {
        boolean playerExists = PlayerUtils.getPlayer(playerName, player -> {
            if (!player.isBanned()) {
                MessageUtils.buildMessage(sender, "&cPlayer &7is not banned.");
                return;
            }
            ProfileBanList profileBanList = Bukkit.getBanList(BanList.Type.PROFILE);
            profileBanList.pardon(player.getPlayerProfile());
            MessageUtils.buildMessage(sender, "Unbanned &a" + playerName);
        });

        if (!playerExists) {
            MessageUtils.buildMessage(sender, "&cPlayer &7not found.");
        }
    }

    /**
     * Get a player by name, whether online or offline.
     *
     * An offline player counts if the player has played on the server before. They are added to the database.
     * @param playerName The name of the player.
     * @param playerConsumer The consumer to run on the player.
     * @return True if the player was found, false otherwise.
     */
    private static boolean getPlayer(String playerName, Consumer<OfflinePlayer> playerConsumer) {
        // First, try to find the Player object in the server. That means the player is online.
        Optional<OfflinePlayer> playerOptional = Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.getName().equalsIgnoreCase(playerName))
                .map(player -> (OfflinePlayer) player)
                .findFirst();
        if (playerOptional.isPresent()) {
            playerConsumer.accept(playerOptional.get());
            return true;
        }

        // If still not found, try looking up the player by their last known name in the database.
        CompletableFuture<Optional<OfflinePlayer>> offlinePlayerFuture = PlayerSentry.getInstance().getOfflinePlayerManager().getOfflinePlayerByName(playerName);
        try {
            Optional<OfflinePlayer> offlinePlayerOptional = offlinePlayerFuture.get(1, TimeUnit.SECONDS); // TODO: Configurable timeout
            if (offlinePlayerOptional.isPresent()) {
                playerConsumer.accept(offlinePlayerOptional.get());
                return true;
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            // Not logging this. It is expected to happen if there is no player by that name that has played on the server before.
            return false;
        }

        return false;
    }

    /**
     * Get the IP address of a player. Whether the player is online or offline.
     * If the player is online, the IP address is retrieved from the player's object.
     * If the player is offline, the IP address is retrieved from the database.
     * TODO: Maybe rework this method a little bit. It is getting quite messy.
     *
     * @param playerName The name of the player to get the IP address of.
     * @return A future that completes with the IP address of the player.
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