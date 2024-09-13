package com.eliaseeg.playersentry.bantypes;

import com.eliaseeg.playersentry.PlayerSentry;
import com.eliaseeg.playersentry.utils.GeneralUtils;
import com.eliaseeg.playersentry.utils.MessageUtils;
import com.eliaseeg.playersentry.utils.PlayerUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Date;
import java.util.Calendar;

/** Represents a ban for a PLAYER for a fixed duration from the server. */
public class TempBan extends BaseBanType {

    public TempBan(String banCommand, String unbanCommand) {
        super(banCommand, unbanCommand);
    }

    @Override
    public void handleBan(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            MessageUtils.buildMessage(sender, "&7Usage: /stempban <player> <time> <reason>");
            return;
        }

        String playerName = args[0];
        String time = args[1].toLowerCase();
        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        Date expirationDate = GeneralUtils.calculateExpirationDate(time);
        if (expirationDate == null) {
            MessageUtils.buildMessage(sender, "&7Invalid time format. Use combinations of d (days), h (hours), and m (minutes).");
            return;
        }

        PlayerUtils.banPlayer(sender, playerName, reason, expirationDate).thenAccept(success -> {
            if (!success) return;
            PlayerUtils.getPlayer(playerName).thenAccept(optionalPlayer -> {
                OfflinePlayer player = optionalPlayer.get();

                PlayerSentry.getInstance().getAuditLogManager().addAuditLog(
                    sender.getName(),
                    player.getUniqueId(),
                    reason,
                    time,
                    this.toString(),
                    false
                );
            });
        });
    }

    @Override
    public void handleUnban(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            MessageUtils.buildMessage(sender, "&7Usage: /stempunban <player>");
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
                        "Temporary ban removed",
                        "N/A",
                        this.toString(),
                        true
                );
            });
        });

    }

    @Override
    public String toString() {
        return "Temporary PLAYER Ban";
    }

}
