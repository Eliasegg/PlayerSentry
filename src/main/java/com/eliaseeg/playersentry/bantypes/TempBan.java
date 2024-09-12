package com.eliaseeg.playersentry.bantypes;

import com.eliaseeg.playersentry.utils.MessageUtils;
import com.eliaseeg.playersentry.utils.PlayerUtils;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.ban.ProfileBanList;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.Date;
import java.util.Calendar;

public class TempBan extends BaseBanType {

    public TempBan(String banCommand, String unbanCommand, String banMessage) {
        super(banCommand, unbanCommand, banMessage);
    }

    @Override
    public void handleBan(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 3) {
            MessageUtils.buildMessage(sender, "&7Usage: /stempban <player> <time> <reason>");
            return;
        }

        String playerName = args[0];
        String time = args[1].toLowerCase();
        String reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));

        Date expirationDate = calculateExpirationDate(time);
        if (expirationDate == null) {
            MessageUtils.buildMessage(sender, "&7Invalid time format. Use combinations of d (days), h (hours), and m (minutes).");
            return;
        }

        PlayerUtils.banPlayer(sender, playerName, reason, expirationDate);
    }

    private Date calculateExpirationDate(String time) {
        int days = 0, hours = 0, minutes = 0;
        String[] parts = time.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)");

        for (int i = 0; i < parts.length; i += 2) {
            if (i + 1 >= parts.length) break;
            int value = Integer.parseInt(parts[i]);
            char unit = parts[i + 1].charAt(0);
            switch (unit) {
                case 'd': days += value; break;
                case 'h': hours += value; break;
                case 'm': minutes += value; break;
                default: return null; // Invalid unit
            }
        }

        if (days == 0 && hours == 0 && minutes == 0) return null; // No valid time specified

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, days);
        calendar.add(Calendar.HOUR_OF_DAY, hours);
        calendar.add(Calendar.MINUTE, minutes);
        return calendar.getTime();
    }

    @Override
    public void handleUnban(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            MessageUtils.buildMessage(sender, "&7Please specify a &cplayer to unban.");
            return;
        }

        String playerName = args[0];
        PlayerUtils.unbanPlayer(sender, playerName);
    }

}
