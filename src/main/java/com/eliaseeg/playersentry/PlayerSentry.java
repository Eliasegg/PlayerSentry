package com.eliaseeg.playersentry;

import com.eliaseeg.playersentry.bantypes.Ban;
import com.eliaseeg.playersentry.bantypes.BaseBanType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public final class PlayerSentry extends JavaPlugin {

    @Override
    public void onEnable() {
        this.registerBanTypes();
        this.saveDefaultConfig();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private void registerBanTypes() {
        BaseBanType[] banTypes = new BaseBanType[]{
                new Ban("sban", "sunban", "You have been banned from this server."),
        };

        Arrays.stream(banTypes)
                .forEach(banType -> {
                    getCommand(banType.getBanCommand()).setExecutor(banType);
                    getCommand(banType.getUnbanCommand()).setExecutor(banType);
                });
    }
}
