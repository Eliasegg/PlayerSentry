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
            MessageUtils.buildMessage(sender, "&cPlayer &7not found.");
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

        // If not online, try to get the OfflinePlayer synchronously first
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
        if (offlinePlayer != null && offlinePlayer.hasPlayedBefore()) {
            playerConsumer.accept(offlinePlayer);
            return true;
        }

        // If still not found, try the async method with a timeout
        CompletableFuture<Optional<OfflinePlayer>> offlinePlayerFuture = PlayerSentry.getInstance().getOfflinePlayerManager().getOfflinePlayerByName(playerName);
        try {
            Optional<OfflinePlayer> offlinePlayerOptional = offlinePlayerFuture.get(5, TimeUnit.SECONDS);
            if (offlinePlayerOptional.isPresent()) {
                playerConsumer.accept(offlinePlayerOptional.get());
                return true;
            }
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            PlayerSentry.getInstance().getLogger().log(Level.SEVERE, "Error or timeout while getting offline player", e);
            return false;
        }

        return false;
    }

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