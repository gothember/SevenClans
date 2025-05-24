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

public class ClanDeclineCommand implements CommandExecutor {

    private final ClanManager clanManager;

    public ClanDeclineCommand(ClanPlugin plugin) {
        this.clanManager = ClanPlugin.getClanManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Usage: /clandecline <clanName>");
            return false; // Shows usage from plugin.yml
        }

        String clanNameToDecline = args[0];
        String pendingInviteClanName = clanManager.getInvite(player.getUniqueId());

        if (pendingInviteClanName == null) {
            player.sendMessage(ChatColor.RED + "You do not have any pending clan invitations.");
            return true;
        }

        if (!pendingInviteClanName.equalsIgnoreCase(clanNameToDecline)) {
            player.sendMessage(ChatColor.RED + "You do not have an invitation from '" + clanNameToDecline + "'. Your current invite is from '" + pendingInviteClanName + "'.");
            player.sendMessage(ChatColor.RED + "Type " + ChatColor.GOLD + "/clandecline " + pendingInviteClanName + ChatColor.RED + " to decline it.");
            return true;
        }

        Clan clan = clanManager.getClan(pendingInviteClanName); // Get clan to notify owner

        if (clanManager.removeInvite(player.getUniqueId())) {
            player.sendMessage(ChatColor.GREEN + "You have declined the invitation from clan '" + pendingInviteClanName + "'.");

            // Notify the clan owner who sent the invite (if they are online)
            if (clan != null) {
                Player clanOwner = Bukkit.getPlayer(clan.getOwner());
                if (clanOwner != null && clanOwner.isOnline()) {
                    clanOwner.sendMessage(ChatColor.YELLOW + player.getName() + " has declined your invitation to join " + clan.getName() + ".");
                }
            }
        } else {
            // Should not happen if previous checks passed
            player.sendMessage(ChatColor.RED + "Failed to decline invitation. It might have already been rescinded or expired.");
        }

        return true;
    }
}
