package com.eliaseeg.playersentry.utils;

import com.eliaseeg.playersentry.PlayerSentry;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class MessageUtils {

    public static void sendMessage(CommandSender sender, String messageKey) {
        sender.sendMessage(getMessage("CHAT_PREFIX") + getMessage(messageKey));
    }

    public static String getMessage(String messageKey) {
        String message = PlayerSentry.getInstance().getConfig().getString("MESSAGES." + messageKey);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static void buildMessage(CommandSender sender, String message) {
        sender.sendMessage(getMessage("CHAT_PREFIX") + ChatColor.translateAlternateColorCodes('&', message));
    }
}
