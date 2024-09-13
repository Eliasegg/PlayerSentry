package com.eliaseeg.playersentry.listeners;

import com.eliaseeg.playersentry.PlayerSentry;
import com.eliaseeg.playersentry.utils.MessageUtils;
import com.eliaseeg.playersentry.database.MutedPlayerManager.MuteInfo;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.Bukkit;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class ChatListener implements Listener {

    // Low priority to allow other plugins to handle chat events first
    @EventHandler(priority = EventPriority.LOW)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        event.setCancelled(true);

        PlayerSentry.getInstance().getMutedPlayerManager().getMuteInfo(player.getUniqueId()).thenAccept(optionalMuteInfo -> {
            Bukkit.getScheduler().runTask(PlayerSentry.getInstance(), () -> {
                if (!optionalMuteInfo.isPresent()) {
                    sendMessage(event);
                    return;
                }

                MuteInfo muteInfo = optionalMuteInfo.get();
                if (!muteInfo.isPermanent() && muteInfo.getExpiresAt().before(new Date())) {
                    sendMessage(event);
                    return;
                }

                String muteMessage = buildMuteMessage(muteInfo);
                MessageUtils.buildMessage(player, muteMessage);
            });
        });
    }

    private void sendMessage(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        String formattedMessage = String.format(event.getFormat(), player.getDisplayName(), message);
        for (Player recipient : event.getRecipients()) {
            recipient.sendMessage(formattedMessage);
        }
    }

    private String buildMuteMessage(MuteInfo muteInfo) {
        StringBuilder message = new StringBuilder("&cYou are muted. ");
        message.append("&7Reason: &f").append(muteInfo.getReason());
        
        if (muteInfo.isPermanent()) {
            message.append(" &7(Permanent)");
        } else {
            long remainingTime = muteInfo.getExpiresAt().getTime() - System.currentTimeMillis();
            String formattedTime = formatDuration(remainingTime);
            message.append(" &7Time remaining: &f").append(formattedTime);
        }
        
        return message.toString();
    }

    private String formatDuration(long millis) {
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        long hours = TimeUnit.MILLISECONDS.toHours(millis) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0) sb.append(seconds).append("s");

        return sb.toString().trim();
    }
}
