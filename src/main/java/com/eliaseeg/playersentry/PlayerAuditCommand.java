package com.eliaseeg.playersentry;

import com.eliaseeg.playersentry.utils.PlayerUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class PlayerAuditCommand implements CommandExecutor {

    // slogs <player> <page>
    @Override
    public boolean onCommand(CommandSender commandSender, Command command, String s, String[] strings) {
        if (strings.length < 1) {
            commandSender.sendMessage("Usage: /slogs <player> <page>");
            return true;
        }

        String playerName = strings[0];
        int page = strings.length > 1 ? Integer.parseInt(strings[1]) : 1;
        PlayerUtils.getAuditLogs(commandSender, playerName, page);
        return true;
    }
}
