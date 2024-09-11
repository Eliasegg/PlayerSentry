package com.eliaseeg.playersentry.utils;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;

public class PlayerUtils {

    /**
     * Get a player by name, whether online or offline.
     * @param playerName The name of the player.
     * @param playerConsumer The consumer to run on the player.
     * @return True if the player was found, false otherwise.
     */
    public static boolean getPlayer(String playerName, Consumer<OfflinePlayer> playerConsumer) {
        Optional<OfflinePlayer> playerOptional = Arrays.stream(Bukkit.getOfflinePlayers())
                .filter(player -> player.getName().equalsIgnoreCase(playerName))
                .findFirst();

        if (playerOptional.isPresent()) {
            playerConsumer.accept(playerOptional.get());
            return true;
        }
        return false;
    }
}