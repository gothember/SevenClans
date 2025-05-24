package com.example.clanplugin.commands;

import com.example.clanplugin.ClanPlugin;
import com.example.clanplugin.clans.Clan;
import com.example.clanplugin.clans.ClanManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.UUID; // Required for iterating members

public class ClanDisbandCommand implements CommandExecutor {

    private final ClanManager clanManager;

    public ClanDisbandCommand(ClanPlugin plugin) {
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

        if (!clan.isOwner(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "Only the clan owner can disband the clan.");
            return true;
        }

        // Optional: Add a confirmation step here, e.g., /clan disband confirm
        // For now, direct disband.

        // Notify members before disbanding
        for (UUID memberUUID : clan.getMembers()) {
            if (!memberUUID.equals(player.getUniqueId())) { // Don't notify the owner about their own action directly like this
                Player member = Bukkit.getPlayer(memberUUID);
                if (member != null && member.isOnline()) {
                    member.sendMessage(ChatColor.RED + "The clan '" + clan.getName() + "' has been disbanded by the owner.");
                }
            }
        }
        
        String clanName = clan.getName(); // Get name before it's deleted

        if (clanManager.deleteClan(clan.getName(), player.getUniqueId())) {
            player.sendMessage(ChatColor.GREEN + "You have successfully disbanded your clan: " + clanName + ".");
            // Note: The deleteClan method in ClanManager already handles removing players from playerClanMap
            // and saving the updated clans data.
        } else {
            // This should not happen if previous checks are correct
            player.sendMessage(ChatColor.RED + "Failed to disband the clan. Please try again.");
        }

        return true;
    }
}
