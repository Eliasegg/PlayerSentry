package com.eliaseeg.playersentry.bantypes;

import com.eliaseeg.playersentry.utils.MessageUtils;
import com.eliaseeg.playersentry.utils.PlayerUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Date;

public class Mute extends BaseBanType {

    public Mute(String banCommand, String unbanCommand) {
        super(banCommand, unbanCommand);
    }

    @Override
    public void handleBan(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            MessageUtils.buildMessage(sender, "&7Usage: /smute <player> <reason>");
            return;
        }

        String playerName = args[0];
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        PlayerUtils.mutePlayer(sender, playerName, reason, (Date) null);
    }

    @Override
    public void handleUnban(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            MessageUtils.buildMessage(sender, "&7Usage: /sunmute <player>");
            return;
        }

        String playerName = args[0];
        PlayerUtils.unmutePlayer(sender, playerName);
    }
}
