package com.example.clanplugin.commands;

import com.example.clanplugin.ClanPlugin;
import com.example.clanplugin.clans.Clan;
import com.example.clanplugin.clans.ClanManager;
import com.example.clanplugin.economy.EconomyManager; // For formatting
import com.example.clanplugin.utils.ColorUtils; // Import ColorUtils
// import org.bukkit.ChatColor; // No longer needed
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClanBalanceCommand implements CommandExecutor {

    private final ClanManager clanManager;

    public ClanBalanceCommand(ClanPlugin plugin) {
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

        double balance = clan.getBalance();
        String balanceMsg = ColorUtils.getConfigMessage("messages.clanBalance", "&bYour clan ({clanName}) treasury balance: &6{balance}")
            .replace("{clanName}", clan.getName())
            .replace("{balance}", EconomyManager.format(balance));
        player.sendMessage(balanceMsg);

        return true;
    }
}
