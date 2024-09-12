package com.eliaseeg.playersentry;

import com.eliaseeg.playersentry.bantypes.Ban;
import com.eliaseeg.playersentry.bantypes.BaseBanType;
import com.eliaseeg.playersentry.bantypes.TempBan;
import com.eliaseeg.playersentry.utils.MessageUtils;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public final class PlayerSentry extends JavaPlugin {

    private static PlayerSentry instance;

    @Override
    public void onEnable() {
        instance = this;

        this.saveDefaultConfig();
        this.registerBanTypes();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    private void registerBanTypes() {
        BaseBanType[] banTypes = new BaseBanType[]{
                new Ban("sban", "sunban", MessageUtils.getMessage("PERMANENT_BAN_MESSAGE")),
                new TempBan("stempban", "stempunban", MessageUtils.getMessage("TEMPORARY_BAN_MESSAGE"))
        };

        Arrays.stream(banTypes)
                .forEach(banType -> {
                    getCommand(banType.getBanCommand()).setExecutor(banType);
                    getCommand(banType.getUnbanCommand()).setExecutor(banType);
                });
    }

    public static PlayerSentry getInstance() {
        return instance;
    }

}
