package com.example.clanplugin.commands;

import com.example.clanplugin.ClanPlugin;
import com.example.clanplugin.clans.Clan;
import com.example.clanplugin.clans.ClanManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit; // For notifying clan owner/members
import java.util.UUID; // For UUID iteration

public class ClanAcceptCommand implements CommandExecutor {

    private final ClanManager clanManager;
    private final ClanPlugin plugin;

    public ClanAcceptCommand(ClanPlugin plugin) {
        this.plugin = plugin;
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
            player.sendMessage(ChatColor.RED + "Usage: /clanaccept <clanName>");
            return false; // Shows usage from plugin.yml
        }

        String clanNameToAccept = args[0];
        String pendingInviteClanName = clanManager.getInvite(player.getUniqueId());

        if (pendingInviteClanName == null) {
            player.sendMessage(ChatColor.RED + "You do not have any pending clan invitations.");
            return true;
        }

        if (!pendingInviteClanName.equalsIgnoreCase(clanNameToAccept)) {
            player.sendMessage(ChatColor.RED + "You do not have an invitation from '" + clanNameToAccept + "'. Your current invite is from '" + pendingInviteClanName + "'.");
            player.sendMessage(ChatColor.RED + "Type " + ChatColor.GOLD + "/clanaccept " + pendingInviteClanName + ChatColor.RED + " to accept it.");
            return true;
        }

        Clan clan = clanManager.getClan(pendingInviteClanName);
        if (clan == null) {
            // This should ideally not happen if an invite exists from a valid clan
            player.sendMessage(ChatColor.RED + "The clan '" + pendingInviteClanName + "' no longer exists.");
            clanManager.removeInvite(player.getUniqueId()); // Clean up stale invite
            return true;
        }
        
        if (clanManager.getClanByPlayer(player.getUniqueId()) != null) {
            player.sendMessage(ChatColor.RED + "You are already in a clan. Please leave your current clan first.");
            // Potentially remove invite here if they are already in another clan.
            // clanManager.removeInvite(player.getUniqueId()); 
            return true;
        }

        // Check member limit from config.yml
        int maxMembers = plugin.getConfig().getInt("clan.maxMembers", 20);
        if (clan.getMembers().size() >= maxMembers) {
            player.sendMessage(ChatColor.RED + "The clan '" + clan.getName() + "' has reached its maximum member limit (" + maxMembers + ").");
            // Notify clan owner?
            Player clanOwner = Bukkit.getPlayer(clan.getOwner());
            if(clanOwner != null && clanOwner.isOnline()){
                clanOwner.sendMessage(ChatColor.YELLOW + player.getName() + " tried to accept your invite, but your clan is full.");
            }
            clanManager.removeInvite(player.getUniqueId()); // Remove invite as it cannot be fulfilled
            return true;
        }

        // Attempt to add player to clan (this will also remove the invite)
        if (clanManager.addPlayerToClan(clan, player.getUniqueId())) {
            player.sendMessage(ChatColor.GREEN + "You have successfully joined the clan: " + clan.getName() + "!");

            // Notify clan owner (and potentially other members)
            Player clanOwner = Bukkit.getPlayer(clan.getOwner());
            if (clanOwner != null && clanOwner.isOnline()) {
                clanOwner.sendMessage(ChatColor.GREEN + player.getName() + " has accepted the invitation and joined your clan!");
            }
            // Broadcast to other clan members if desired
            for (UUID memberUUID : clan.getMembers()) {
                if (!memberUUID.equals(player.getUniqueId()) && !memberUUID.equals(clan.getOwner())) { // Don't re-notify self or owner
                    Player member = Bukkit.getPlayer(memberUUID);
                    if (member != null && member.isOnline()) {
                        member.sendMessage(ChatColor.AQUA + player.getName() + " has joined the clan!");
                    }
                }
            }

        } else {
            // This might happen due to race conditions or other unexpected issues
            player.sendMessage(ChatColor.RED + "Failed to join the clan. Please try again.");
        }

        return true;
    }
}
