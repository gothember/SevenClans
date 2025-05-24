package com.example.clanplugin.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import java.util.logging.Logger;

public class EconomyManager {

    private static Economy econ = null;
    private static final Logger log = Bukkit.getLogger();
    private static String pluginName = ""; // To store the name of the plugin using this manager

    public EconomyManager(String pluginName) {
        EconomyManager.pluginName = pluginName;
        if (!setupEconomy() ) {
            log.severe(String.format("[%s] - Disabled due to no Vault dependency found!", pluginName));
            Bukkit.getServer().getPluginManager().disablePlugin(Bukkit.getServer().getPluginManager().getPlugin(pluginName));
        }
    }

    private boolean setupEconomy() {
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
            log.warning(String.format("[%s] Vault plugin not found. Economy features will not be available.", pluginName));
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            log.warning(String.format("[%s] No economy provider found through Vault. Economy features will not be available.", pluginName));
            return false;
        }
        econ = rsp.getProvider();
        log.info(String.format("[%s] Hooked into Vault and found economy provider: %s", pluginName, econ.getName()));
        return econ != null;
    }

    public static Economy getEconomy() {
        return econ;
    }

    public static boolean hasEnough(OfflinePlayer player, double amount) {
        if (econ == null) return false;
        return econ.has(player, amount);
    }

    public static boolean withdrawMoney(OfflinePlayer player, double amount) {
        if (econ == null) return false;
        EconomyResponse r = econ.withdrawPlayer(player, amount);
        return r.transactionSuccess();
    }

    public static boolean depositMoney(OfflinePlayer player, double amount) {
        if (econ == null) return false;
        EconomyResponse r = econ.depositPlayer(player, amount);
        return r.transactionSuccess();
    }

    public static String format(double amount) {
        if (econ == null) return String.valueOf(amount);
        return econ.format(amount);
    }
}
