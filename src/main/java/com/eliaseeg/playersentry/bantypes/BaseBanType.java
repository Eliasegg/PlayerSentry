package com.eliaseeg.playersentry.bantypes;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;

public abstract class BaseBanType implements CommandExecutor, Listener {

    protected String banCommand;
    protected String unbanCommand;
    protected String banMessage;

    public BaseBanType(String banCommand, String unbanCommand, String banMessage) {
        this.banCommand = banCommand;
        this.unbanCommand = unbanCommand;
        this.banMessage = banMessage;
    }

    public abstract void handleBan(CommandSender sender, Command command, String label, String[] args);
    public abstract void handleUnban(CommandSender sender, Command command, String label, String[] args);

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase(banCommand)) {
            handleBan(sender, command, label, args);
            return true;
        } else if (command.getName().equalsIgnoreCase(unbanCommand)) {
            handleUnban(sender, command, label, args);
            return true;
        }
        return false;
    }

    public String getBanCommand() {
        return banCommand;
    }

    public String getUnbanCommand() {
        return unbanCommand;
    }

    public String getBanMessage() {
        return banMessage;
    }

}
