package com.eliaseeg.playersentry.bantypes;

import com.eliaseeg.playersentry.utils.MessageUtils;
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
            MessageUtils.buildMessage(sender, "&7Please specify a &cplayer to ban.");
            return;
        }

        if (args.length > 1) {
            MessageUtils.buildMessage(sender, "&cToo many arguments. &7Try " + this.banCommand + " <player>");
            return;
        }

        String playerName = args[0];
        boolean playerExists = PlayerUtils.getPlayer(playerName, player -> {

            if (player.isBanned()) {
                MessageUtils.buildMessage(sender, "&cPlayer &7is already banned.");
                return;
            }

            ProfileBanList profileBanList = Bukkit.getBanList(BanList.Type.PROFILE);
            profileBanList.addBan(player.getPlayerProfile(), banMessage, (Date) null, sender.getName());

            if (player.isOnline()) {
                ((Player)player).kickPlayer(banMessage);
            }

            MessageUtils.buildMessage(sender, "Banned &a" + playerName);
        });

        if (!playerExists) {
            MessageUtils.buildMessage(sender, "&cPlayer &7not found.");
        }
    }

    @Override
    public void handleUnban(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            MessageUtils.buildMessage(sender, "&7Please specify a &cplayer to unban.");
            return;
        }

        if (args.length > 1) {
            MessageUtils.buildMessage(sender, "&cToo many arguments. &7Try " + this.unbanCommand + " <player>");
            return;
        }

        String playerName = args[0];

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

}
