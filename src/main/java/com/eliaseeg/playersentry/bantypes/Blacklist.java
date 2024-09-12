package com.eliaseeg.playersentry.bantypes;

import com.eliaseeg.playersentry.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

public class Blacklist extends BaseBanType {

    public Blacklist(String banCommand, String unbanCommand) {
        super(banCommand, unbanCommand);
    }

    @Override
    public void handleBan(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 3) {
            MessageUtils.buildMessage(sender, "&7Usage: /sblacklist <player> <reason>");
            return;
        }

        String playerName = args[0];

    }

    @Override
    public void handleUnban(CommandSender sender, Command command, String label, String[] args) {

    }
}
