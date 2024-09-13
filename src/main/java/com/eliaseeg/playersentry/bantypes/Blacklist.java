package com.eliaseeg.playersentry.bantypes;

import com.eliaseeg.playersentry.utils.MessageUtils;
import com.eliaseeg.playersentry.utils.PlayerUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

/** Represents a ban for an IP address permanently from the server. */
public class Blacklist extends BaseBanType {

    public Blacklist(String banCommand, String unbanCommand) {
        super(banCommand, unbanCommand);
    }

    @Override
    public void handleBan(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            MessageUtils.buildMessage(sender, "&7Usage: /sblacklist <player> <reason>");
            return;
        }

        String playerName = args[0];
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        PlayerUtils.banPlayerIP(sender, playerName, reason);
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
        PlayerUtils.unbanPlayerIP(sender, playerName);
    }
}
