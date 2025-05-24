package com.example.clanplugin.commands;

import com.example.clanplugin.ClanPlugin;
import com.example.clanplugin.clans.Clan;
import com.example.clanplugin.clans.ClanManager;
import com.example.clanplugin.economy.EconomyManager;
import com.example.clanplugin.utils.ColorUtils; // Import ColorUtils
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
// import org.bukkit.ChatColor; // No longer needed directly for messages
import org.bukkit.configuration.file.FileConfiguration; // To get config values

public class ClanCreateCommand implements CommandExecutor {

    private final ClanPlugin plugin;
    private final ClanManager clanManager;
    // EconomyManager has static methods, direct calls are fine e.g. EconomyManager.hasEnough
    // No need for an instance variable 'economyManager' if all methods used are static.

    public ClanCreateCommand(ClanPlugin plugin) {
        this.plugin = plugin;
        this.clanManager = ClanPlugin.getClanManager();
        // this.economyManager = ClanPlugin.getEconomyManager(); // Not needed if using static methods
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtils.getRawConfigMessage("messages.error.playerOnlyCommand", "&cThis command can only be run by a player."));
            return true;
        }

        Player player = (Player) sender;
        FileConfiguration config = plugin.getConfig(); 

        if (args.length == 0) {
            player.sendMessage(ColorUtils.translateColorCodes(plugin.getCommand(command.getName()).getUsage())); // Get usage from plugin.yml
            return false; 
        }

        String clanName = args[0];

        if (clanManager.getClanByPlayer(player.getUniqueId()) != null) {
            player.sendMessage(ColorUtils.getConfigMessage("messages.error.alreadyInClan", "&cYou are already in a clan."));
            return true;
        }

        int minLength = config.getInt("clan.name.minLength", 3);
        int maxLength = config.getInt("clan.name.maxLength", 16);
        String disallowedCharsPattern = config.getString("clan.name.disallowedRegex", "[^a-zA-Z0-9_]");

        if (clanName.length() < minLength || clanName.length() > maxLength) {
            // Example of a message not directly in config, but could be made so
            String lengthError = ColorUtils.getRawConfigMessage("messages.error.clanNameLength", "&cClan name must be between {min} and {max} characters long.")
                .replace("{min}", String.valueOf(minLength))
                .replace("{max}", String.valueOf(maxLength));
            player.sendMessage(ColorUtils.getConfigMessage("messages.prefix", "") + lengthError); // Manually adding prefix for this construction
            return true;
        }
        
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(disallowedCharsPattern);
        if (pattern.matcher(clanName).find()) {
             player.sendMessage(ColorUtils.getConfigMessage("messages.error.invalidClanName", "&cClan name contains invalid characters. Use A-Z, a-z, 0-9, _"));
             return true;
        }
        
        if (clanManager.getClan(clanName) != null) {
            player.sendMessage(ColorUtils.getConfigMessage("messages.error.clanAlreadyExists", "&cA clan with this name already exists.").replace("{clanName}", clanName));
            return true;
        }

        double creationCost = config.getDouble("clan.creation.minMoney", 1000.0);
        if (!EconomyManager.hasEnough(player, creationCost)) {
            String noMoneyMsg = ColorUtils.getConfigMessage("messages.error.notEnoughMoney", "&cYou do not have enough money. Cost: {cost}");
            player.sendMessage(noMoneyMsg.replace("{cost}", EconomyManager.format(creationCost)));
            return true;
        }

        if (!EconomyManager.withdrawMoney(player, creationCost)) {
            player.sendMessage(ColorUtils.getConfigMessage("messages.error.economyError", "&cFailed to withdraw money. Please try again."));
            return true;
        }

        Clan newClan = clanManager.createClan(clanName, player.getUniqueId());
        if (newClan == null) {
            EconomyManager.depositMoney(player, creationCost); // Refund
            player.sendMessage(ColorUtils.getConfigMessage("messages.error.generic", "&cFailed to create clan. Money refunded.")); // A more generic error
            return true;
        }

        String createdMsg = ColorUtils.getConfigMessage("messages.clanCreated", "&#00FF00Clan '&{clanName}&#00FF00' created successfully!");
        player.sendMessage(createdMsg.replace("{clanName}", newClan.getName()));
        return true;
    }
}
