package com.eliaseeg.playersentry.bantypes;

import com.eliaseeg.playersentry.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class Kick extends BaseBanType {

    public Kick(String banCommand, String unbanCommand) {
        super(banCommand, unbanCommand);
    }

    @Override
    public void handleBan(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            MessageUtils.buildMessage(sender, "&7Usage: /skick <player> <reason>");
            return;
        }

        String playerName = args[0];
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            MessageUtils.buildMessage(sender, "&cPlayer &7not found. Perhaps they are not online?");
            return;
        }

        player.kickPlayer(ChatColor.translateAlternateColorCodes('&', reason));
        MessageUtils.buildMessage(sender, "&aPlayer &7kicked.");
    }

    @Override
    public void handleUnban(CommandSender sender, Command command, String label, String[] args) {
        return; // Kick should not have an unban command as there is no opposite to kick.
    }


}
