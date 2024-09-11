package com.eliaseeg.playersentry.bantypes;

import com.eliaseeg.playersentry.utils.PlayerUtils;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ban.ProfileBanList;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Date;

/**
 * Represents a Ban for a PLAYER permanently from the server.
 */
public class Ban extends BaseBanType {

    public Ban(String banCommand, String unbanCommand, String banMessage) {
        super(banCommand, unbanCommand, banMessage);
    }

    @Override
    public void handleBan(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Please specify a player to ban.");
            return;
        }

        if (args.length > 1) {
            sender.sendMessage("Too many arguments.");
            return;
        }

        String playerName = args[0];

        // Ban offline player
        boolean playerExists = PlayerUtils.getPlayer(playerName, player -> {
            if (player.isBanned()) {
                sender.sendMessage("Player is already banned.");
                return;
            }
            ProfileBanList profileBanList = Bukkit.getBanList(BanList.Type.PROFILE);
            profileBanList.addBan(player.getPlayerProfile(), banMessage, (Date) null, sender.getName());
            if (player.isOnline()) {
                ((Player)player).kickPlayer(banMessage);
            }
            sender.sendMessage("Banned " + playerName);
        });

        if (!playerExists) {
            sender.sendMessage("Player not found.");
        }
    }

    @Override
    public void handleUnban(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("Please specify a player to unban.");
            return;
        }

        if (args.length > 1) {
            sender.sendMessage("Too many arguments.");
            return;
        }

        String playerName = args[0];

        // Unban offline player
        boolean playerExists = PlayerUtils.getPlayer(playerName, player -> {
            if (!player.isBanned()) {
                sender.sendMessage("Player is not banned.");
                return;
            }
            ProfileBanList profileBanList = Bukkit.getBanList(BanList.Type.PROFILE);
            profileBanList.pardon(player.getPlayerProfile());
            sender.sendMessage("Unbanned " + playerName);
        });

        if (!playerExists) {
            sender.sendMessage("Player not found.");
        }
    }

}
