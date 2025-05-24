package com.example.clanplugin.commands;

import com.example.clanplugin.ClanPlugin;
import com.example.clanplugin.clans.Clan;
import com.example.clanplugin.clans.ClanManager;
import com.example.clanplugin.economy.EconomyManager;
import com.example.clanplugin.utils.ColorUtils; // Import ColorUtils
// import org.bukkit.ChatColor; // No longer needed
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClanInvestCommand implements CommandExecutor {

    private final ClanManager clanManager;
    // EconomyManager is static

    public ClanInvestCommand(ClanPlugin plugin) {
        this.clanManager = ClanPlugin.getClanManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ColorUtils.getRawConfigMessage("messages.error.playerOnlyCommand", "&cThis command can only be run by a player."));
            return true;
        }

        Player player = (Player) sender;
        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());

        if (clan == null) {
            player.sendMessage(ColorUtils.getConfigMessage("messages.error.clanOnlyCommand", "&cYou must be in a clan to use this command."));
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(ColorUtils.translateColorCodes("&cUsage: /claninvest <amount>")); // Or get from plugin.yml
            return false;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            player.sendMessage(ColorUtils.getConfigMessage("messages.error.invalidAmount", "&cInvalid amount specified. Please enter a number."));
            return true;
        }

        if (amount <= 0) {
            player.sendMessage(ColorUtils.getConfigMessage("messages.error.amountMustBePositive", "&cAmount must be positive."));
            return true;
        }

        if (!EconomyManager.hasEnough(player, amount)) {
            String notEnoughPlayerMsg = ColorUtils.getConfigMessage("messages.error.playerNotEnoughMoney", "&cYou do not have enough money. Your balance: {balance}")
                .replace("{balance}", EconomyManager.format(EconomyManager.getEconomy().getBalance(player)));
            player.sendMessage(notEnoughPlayerMsg);
            return true;
        }

        if (EconomyManager.withdrawMoney(player, amount)) {
            clan.setBalance(clan.getBalance() + amount);
            clanManager.saveClans();
            
            String investSuccessMsg = ColorUtils.getConfigMessage("messages.investSuccess", "&aSuccessfully invested {amount} into the clan treasury.")
                .replace("{amount}", EconomyManager.format(amount));
            player.sendMessage(investSuccessMsg);

            String newBalanceMsg = ColorUtils.getConfigMessage("messages.newClanBalance", "&aNew clan balance: {balance}")
                .replace("{balance}", EconomyManager.format(clan.getBalance()));
            player.sendMessage(newBalanceMsg);
        } else {
            player.sendMessage(ColorUtils.getConfigMessage("messages.error.economyError", "&cFailed to withdraw funds from your account. Please try again."));
        }

        return true;
    }
}
