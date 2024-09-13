package com.eliaseeg.playersentry.bantypes;

import com.eliaseeg.playersentry.PlayerSentry;
import com.eliaseeg.playersentry.utils.MessageUtils;
import com.eliaseeg.playersentry.utils.PlayerUtils;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Arrays;

/** Represents a Ban for a PLAYER permanently from the server */
public class Ban extends BaseBanType {

    public Ban(String banCommand, String unbanCommand) {
        super(banCommand, unbanCommand);
    }

    @Override
    public void handleBan(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            MessageUtils.buildMessage(sender, "&7Usage: /sban <player> <reason>");
            return;
        }

        String playerName = args[0];
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        PlayerUtils.banPlayer(sender, playerName, reason, null).thenAccept(success -> {
            if (!success) return;

            PlayerUtils.getPlayer(playerName).thenAccept(optionalPlayer -> {
                OfflinePlayer player = optionalPlayer.get();

                PlayerSentry.getInstance().getAuditLogManager().addAuditLog(
                        sender.getName(),
                        player.getUniqueId(),
                        reason,
                        "Permanent",
                        this.toString(),
                        false
                );
            });
        });

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

        PlayerUtils.unbanPlayer(sender, playerName).thenAccept(success -> {
            if (!success) return;
            PlayerUtils.getPlayer(playerName).thenAccept(optionalPlayer -> {
                OfflinePlayer player = optionalPlayer.get();

                PlayerSentry.getInstance().getAuditLogManager().addAuditLog(
                        sender.getName(),
                        player.getUniqueId(),
                        "Unbanned",
                        "N/A",
                        this.toString(),
                        true
                );
            });
        });

    }

    @Override
    public String toString() {
        return "Permanent PLAYER Ban";
    }

}
