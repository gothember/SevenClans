package com.example.clanplugin.commands;

import com.example.clanplugin.ClanPlugin;
import com.example.clanplugin.clans.Clan;
import com.example.clanplugin.clans.ClanManager;
import com.example.clanplugin.economy.EconomyManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClanWithdrawCommand implements CommandExecutor {

    private final ClanManager clanManager;
    // EconomyManager is static, can be called directly

    public ClanWithdrawCommand(ClanPlugin plugin) {
        this.clanManager = ClanPlugin.getClanManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;
        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());

        if (clan == null) {
            player.sendMessage(ChatColor.RED + "You are not in a clan.");
            return true;
        }

        // For now, only owner can withdraw. This can be expanded to roles with permissions.
        if (!clan.isOwner(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Only the clan owner can withdraw funds.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /clanwithdraw <amount>");
            return false;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Invalid amount specified. Please enter a number.");
            return true;
        }

        if (amount <= 0) {
            player.sendMessage(ChatColor.RED + "Withdrawal amount must be positive.");
            return true;
        }

        if (clan.getBalance() < amount) {
            player.sendMessage(ChatColor.RED + "The clan does not have enough funds. Current balance: " + EconomyManager.format(clan.getBalance()));
            return true;
        }

        // Attempt to deposit money to player's account first
        if (EconomyManager.depositMoney(player, amount)) {
            // If successful, update clan balance
            clan.setBalance(clan.getBalance() - amount);
            clanManager.saveClans(); // Save the updated clan balance
            player.sendMessage(ChatColor.GREEN + "Successfully withdrew " + EconomyManager.format(amount) + " from the clan treasury.");
            player.sendMessage(ChatColor.GREEN + "New clan balance: " + EconomyManager.format(clan.getBalance()));
        } else {
            player.sendMessage(ChatColor.RED + "Failed to transfer funds to your account. Please try again.");
        }

        return true;
    }
}
