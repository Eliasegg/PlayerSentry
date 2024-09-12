package com.eliaseeg.playersentry.listeners;

import com.eliaseeg.playersentry.PlayerSentry;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class QuitListener implements Listener {

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Save the player's IP address and last logged name to the database so they can be retrieved later
        PlayerSentry.getInstance().getOfflinePlayerManager().addOrUpdatePlayer(event.getPlayer().getUniqueId(), event.getPlayer().getAddress().getAddress().getHostAddress(), event.getPlayer().getName());
    }
}
