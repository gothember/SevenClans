package com.example.clanplugin;

import com.example.clanplugin.clans.ClanManager;
import com.example.clanplugin.commands.ClanCreateCommand;
import com.example.clanplugin.commands.ClanInviteCommand; // Already present, ensure it stays
import com.example.clanplugin.commands.ClanAcceptCommand;
import com.example.clanplugin.commands.ClanDeclineCommand;
import com.example.clanplugin.commands.ClanDisbandCommand;
import com.example.clanplugin.commands.ClanWithdrawCommand; // Add this import
import com.example.clanplugin.economy.EconomyManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ClanPlugin extends JavaPlugin {

    private static EconomyManager economyManager;
    private static ClanManager clanManager;
    private static ClanPlugin instance; // Add this line

    @Override
    public void onEnable() {
        instance = this; // Set instance
        // Save default config.yml if it doesn't exist
        saveDefaultConfig();
        
        economyManager = new EconomyManager(this.getDescription().getName());
        if (EconomyManager.getEconomy() == null) {
            // EconomyManager already logs and disables, but this is an additional check.
            getLogger().severe("Failed to initialize EconomyManager and hook into Vault. ClanPlugin economy features will be disabled.");
            getServer().getPluginManager().disablePlugin(this); // Ensure plugin is disabled if econ setup failed
            return;
        }

        clanManager = new ClanManager(this);
        // It's good practice to check if critical components like ClanManager initialized correctly.
        // For example, if ClanManager had a method like `isSuccessfullyInitialized()`.
        // For now, we assume its constructor completes without fatal errors unless it throws one.

        // Register commands
        // Ensure ClanManager is available before registering commands that depend on it.
        if (clanManager == null) { 
            getLogger().severe("ClanManager failed to initialize. Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.getCommand("clancreate").setExecutor(new ClanCreateCommand(this));
        this.getCommand("claninvite").setExecutor(new ClanInviteCommand(this));
        this.getCommand("clanaccept").setExecutor(new ClanAcceptCommand(this));
        this.getCommand("clandecline").setExecutor(new ClanDeclineCommand(this));
        this.getCommand("clandisband").setExecutor(new ClanDisbandCommand(this));
        this.getCommand("clanwithdraw").setExecutor(new ClanWithdrawCommand(this)); // Add this line
        
        getLogger().info("ClanPlugin has been enabled, EconomyManager and ClanManager are initialized, commands registered!");
    }

    @Override
    public void onDisable() {
        if (clanManager != null) {
            clanManager.saveClans();
        }
        getLogger().info("ClanPlugin has been disabled!");
    }

    public static EconomyManager getEconomyManager() {
        return economyManager;
    }

    public static ClanManager getClanManager() {
        return clanManager;
    }

    public static ClanPlugin getInstance() { // Add this method
        return instance;
    }
}
