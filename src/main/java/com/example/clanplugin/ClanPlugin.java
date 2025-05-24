package com.example.clanplugin;

import com.example.clanplugin.clans.ClanManager;
import com.example.clanplugin.commands.ClanCreateCommand;
import com.example.clanplugin.commands.ClanInviteCommand; // Already present, ensure it stays
import com.example.clanplugin.commands.ClanAcceptCommand;
import com.example.clanplugin.commands.ClanDeclineCommand;
import com.example.clanplugin.commands.ClanDisbandCommand;
import com.example.clanplugin.commands.ClanWithdrawCommand;
import com.example.clanplugin.commands.ClanInvestCommand;
import com.example.clanplugin.commands.ClanBalanceCommand;
import com.example.clanplugin.commands.ClanMembersCommand;
import com.example.clanplugin.commands.ClanPvpCommand; // Add this import
import com.example.clanplugin.listeners.PlayerDamageListener; // Add this import
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
        this.getCommand("clanwithdraw").setExecutor(new ClanWithdrawCommand(this));
        this.getCommand("claninvest").setExecutor(new ClanInvestCommand(this));
        this.getCommand("clanbalance").setExecutor(new ClanBalanceCommand(this));
        this.getCommand("clanmembers").setExecutor(new ClanMembersCommand(this));
        this.getCommand("clanpvp").setExecutor(new ClanPvpCommand(this)); // Add this line
        
        // Register listeners
        getServer().getPluginManager().registerEvents(new PlayerDamageListener(this), this);

        // Start scheduler for checking expired invites
        long checkInterval = 20L * 60; // Check every 1 minute (20 ticks * 60 seconds)
        // Consider making this interval configurable in config.yml if desired.
        getServer().getScheduler().runTaskTimer(this, () -> {
            if (clanManager != null) {
                clanManager.checkExpiredInvites();
            }
        }, checkInterval, checkInterval); // Initial delay, period

        getLogger().info("ClanPlugin has been enabled, EconomyManager and ClanManager are initialized, commands, listeners, and tasks registered!");
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
