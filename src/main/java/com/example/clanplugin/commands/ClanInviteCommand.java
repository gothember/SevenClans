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

import java.util.UUID;

public class ClanInviteCommand implements CommandExecutor {

    private final ClanManager clanManager;
    // private final InviteManager inviteManager; // If we create a separate manager

    public ClanInviteCommand(ClanPlugin plugin) {
        this.clanManager = ClanPlugin.getClanManager();
        // this.inviteManager = ClanPlugin.getInviteManager(); 
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be run by a player.");
            return true;
        }

        Player inviter = (Player) sender;

        if (args.length == 0) {
            inviter.sendMessage(ChatColor.RED + "Usage: /claninvite <player>");
            return false; // Shows usage from plugin.yml
        }

        Clan inviterClan = clanManager.getClanByPlayer(inviter.getUniqueId());
        if (inviterClan == null) {
            inviter.sendMessage(ChatColor.RED + "You are not in a clan.");
            return true;
        }

        // Basic permission: only owner can invite (can be expanded later)
        if (!inviterClan.isOwner(inviter.getUniqueId())) {
            inviter.sendMessage(ChatColor.RED + "Only the clan owner can invite new members.");
            return true;
        }

        Player targetPlayer = Bukkit.getPlayerExact(args[0]);
        if (targetPlayer == null) {
            inviter.sendMessage(ChatColor.RED + "Player '" + args[0] + "' not found or is not online.");
            return true;
        }

        if (inviter.getUniqueId().equals(targetPlayer.getUniqueId())) {
            inviter.sendMessage(ChatColor.RED + "You cannot invite yourself.");
            return true;
        }

        if (clanManager.getClanByPlayer(targetPlayer.getUniqueId()) != null) {
            inviter.sendMessage(ChatColor.RED + targetPlayer.getName() + " is already in a clan.");
            return true;
        }
        
        // Check member limit from config.yml
        int maxMembers = ClanPlugin.getInstance().getConfig().getInt("clan.maxMembers", 20);
        if (inviterClan.getMembers().size() >= maxMembers) {
            inviter.sendMessage(ChatColor.RED + "Your clan has reached the maximum number of members (" + maxMembers + ").");
            return true;
        }

        // Store invitation
        boolean success = clanManager.addInvite(targetPlayer.getUniqueId(), inviterClan.getName());
        if (!success) {
             // This could happen if there's already an active invite for this player from another clan,
             // or if the clan name is somehow invalid (though clan object should be valid here)
            inviter.sendMessage(ChatColor.RED + "Could not send invitation. " + targetPlayer.getName() + " might have a pending invite.");
            targetPlayer.sendMessage(ChatColor.YELLOW + inviter.getName() + " tried to invite you to " + inviterClan.getName() + 
                                       ", but you already have a pending invitation.");
            return true;
        }
        
        inviter.sendMessage(ChatColor.GREEN + "Invitation sent to " + targetPlayer.getName() + " to join " + inviterClan.getName() + ".");
        targetPlayer.sendMessage(ChatColor.AQUA + "You have been invited to join the clan '" + inviterClan.getName() + "' by " + inviter.getName() + ".");
        targetPlayer.sendMessage(ChatColor.AQUA + "Type " + ChatColor.GOLD + "/clanaccept " + inviterClan.getName() + ChatColor.AQUA + " to accept or " + 
                                   ChatColor.GOLD + "/clandecline " + inviterClan.getName() + ChatColor.AQUA + " to decline.");

        return true;
    }
}
