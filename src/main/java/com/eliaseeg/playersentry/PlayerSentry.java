package com.eliaseeg.playersentry;

import com.eliaseeg.playersentry.bantypes.Ban;
import com.eliaseeg.playersentry.bantypes.BaseBanType;
import com.eliaseeg.playersentry.bantypes.Blacklist;
import com.eliaseeg.playersentry.bantypes.TempBan;
import com.eliaseeg.playersentry.listeners.QuitListener;
import org.bukkit.plugin.java.JavaPlugin;
import com.eliaseeg.playersentry.database.SQLiteManager;
import com.eliaseeg.playersentry.database.OfflinePlayerManager;
import com.eliaseeg.playersentry.database.MutedPlayerManager;
import com.eliaseeg.playersentry.database.AuditLogManager;

import java.util.Arrays;

public final class PlayerSentry extends JavaPlugin {

    private static PlayerSentry instance;
    private SQLiteManager sqliteManager;
    private OfflinePlayerManager offlinePlayerManager;
    private MutedPlayerManager mutedPlayerManager;
    private AuditLogManager auditLogManager;

    @Override
    public void onEnable() {
        instance = this;

        this.saveDefaultConfig();
        this.registerBanTypes();

        sqliteManager = SQLiteManager.getInstance(this);
        sqliteManager.initialize();

        // managers for each table in the db
        offlinePlayerManager = new OfflinePlayerManager(this);
        mutedPlayerManager = new MutedPlayerManager(this);
        auditLogManager = new AuditLogManager(this);

        this.getServer().getPluginManager().registerEvents(new QuitListener(), this);
    }

    @Override
    public void onDisable() {
        // Close the database connection
        if (sqliteManager != null) {
            sqliteManager.closeConnection();
        }
    }

    private void registerBanTypes() {
        BaseBanType[] banTypes = new BaseBanType[]{
                new Ban("sban", "sunban"),
                new TempBan("stempban", "stempunban"),
                new Blacklist("sblacklist", "sunblacklist"),
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

    public SQLiteManager getSqliteManager() {
        return sqliteManager;
    }

    public OfflinePlayerManager getOfflinePlayerManager() {
        return offlinePlayerManager;
    }

    public MutedPlayerManager getMutedPlayerManager() {
        return mutedPlayerManager;
    }

    public AuditLogManager getAuditLogManager() {
        return auditLogManager;
    }

}
