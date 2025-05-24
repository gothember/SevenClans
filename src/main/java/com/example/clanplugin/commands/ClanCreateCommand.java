package com.example.clanplugin.commands;

import com.example.clanplugin.ClanPlugin;
import com.example.clanplugin.clans.Clan;
import com.example.clanplugin.clans.ClanManager;
import com.example.clanplugin.economy.EconomyManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor; // For colored messages
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
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /clan create <name>");
            return false; // Shows usage from plugin.yml
        }

        String clanName = args[0];
        FileConfiguration config = plugin.getConfig();

        // Check if player is already in a clan
        if (clanManager.getClanByPlayer(player.getUniqueId()) != null) {
            player.sendMessage(ChatColor.RED + "You are already in a clan.");
            return true;
        }

        // Validate clan name
        int minLength = config.getInt("clan.name.minLength", 3);
        int maxLength = config.getInt("clan.name.maxLength", 16);
        // The regex stored in config is for *disallowed* characters.
        // We want to check if the name *contains* any of these disallowed characters.
        String disallowedCharsPattern = config.getString("clan.name.disallowedRegex", "[^a-zA-Z0-9_]");


        if (clanName.length() < minLength || clanName.length() > maxLength) {
            player.sendMessage(ChatColor.RED + "Clan name must be between " + minLength + " and " + maxLength + " characters long.");
            return true;
        }

        // Check against allowed characters. If disallowedRegex is "[^a-zA-Z0-9_]",
        // then a valid name should NOT match "[^a-zA-Z0-9_]".
        // A simpler way is to define what IS allowed, e.g. "^[a-zA-Z0-9_]+$"
        // For this example, let's assume disallowedRegex means "any char NOT in this set is disallowed"
        // No, the provided disallowedRegex `[^a-zA-Z0-9_]` means "match any character that is NOT alphanumeric or underscore".
        // So, if `clanName.matches(".*" + disallowedRegex + ".*")` or similar to check if it *contains* a disallowed char.
        // Let's re-evaluate the logic for validation.
        // The current logic: `!clanName.matches("^[a-zA-Z0-9_]+$")` checks if it's NOT purely alphanumeric underscore.
        // `clanName.matches(disallowedRegex)` checks if the whole string IS a disallowed character (which is wrong).
        // Correct logic: check if the clan name contains any character that is matched by disallowedRegex.
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(disallowedCharsPattern);
        if (pattern.matcher(clanName).find()) {
             player.sendMessage(ChatColor.RED + "Clan name contains invalid characters. Use A-Z, a-z, 0-9, _ only.");
             return true;
        }
        
        if (clanManager.getClan(clanName) != null) {
            player.sendMessage(ChatColor.RED + "A clan with this name already exists.");
            return true;
        }

        // Check player balance
        double creationCost = config.getDouble("clan.creation.minMoney", 1000.0);
        if (!EconomyManager.hasEnough(player, creationCost)) {
            player.sendMessage(ChatColor.RED + "You do not have enough money to create a clan. Cost: " + EconomyManager.format(creationCost));
            return true;
        }

        // Attempt to withdraw money
        if (!EconomyManager.withdrawMoney(player, creationCost)) {
            player.sendMessage(ChatColor.RED + "Failed to withdraw money for clan creation. Please try again.");
            return true;
        }

        // Create clan
        Clan newClan = clanManager.createClan(clanName, player.getUniqueId());
        if (newClan == null) {
            // This could happen if clan name check fails due to case sensitivity differences
            // or if player somehow joined another clan between checks.
            EconomyManager.depositMoney(player, creationCost); // Refund
            player.sendMessage(ChatColor.RED + "Failed to create clan. This name might be taken or you might have joined another clan. Money has been refunded.");
            return true;
        }

        player.sendMessage(ChatColor.GREEN + "Clan '" + newClan.getName() + "' created successfully!");
        return true;
    }
}
