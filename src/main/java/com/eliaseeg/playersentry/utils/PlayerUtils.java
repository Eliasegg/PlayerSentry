package com.eliaseeg.playersentry.utils;

import com.eliaseeg.playersentry.PlayerSentry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.ban.ProfileBanList;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class PlayerUtils {

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

        // If the player is not online, try to find the Player object in the offline player manager.
        Optional<OfflinePlayer> offlinePlayerOptional = PlayerSentry.getInstance().getOfflinePlayerManager().getOfflinePlayerByName(playerName)
                .thenApply(Optional::ofNullable)
                .join();
        if (offlinePlayerOptional.isPresent()) {
            playerConsumer.accept(offlinePlayerOptional.get());
            return true;
        }

        return false;
    }

}