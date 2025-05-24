package com.example.clanplugin.commands;

import com.example.clanplugin.ClanPlugin;
import com.example.clanplugin.clans.Clan;
import com.example.clanplugin.clans.ClanManager;
import com.example.clanplugin.utils.ColorUtils; // Import ColorUtils
import org.bukkit.Bukkit;
// import org.bukkit.ChatColor; // No longer needed for direct messages
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
            sender.sendMessage(ColorUtils.getRawConfigMessage("messages.error.playerOnlyCommand", "&cThis command can only be run by a player."));
            return true;
        }

        Player inviter = (Player) sender;

        if (args.length == 0) {
            inviter.sendMessage(ColorUtils.translateColorCodes("&cUsage: /claninvite <player>")); // Or get from plugin.yml
            return false; 
        }

        Clan inviterClan = clanManager.getClanByPlayer(inviter.getUniqueId());
        if (inviterClan == null) {
            inviter.sendMessage(ColorUtils.getConfigMessage("messages.error.clanOnlyCommand", "&cYou must be in a clan to use this command."));
            return true;
        }

        if (!inviterClan.isOwner(inviter.getUniqueId())) { // Assuming config.yml does not yet have role-based perms
            inviter.sendMessage(ColorUtils.getConfigMessage("messages.error.notClanOwner", "&cOnly the clan owner can perform this action."));
            return true;
        }

        Player targetPlayer = Bukkit.getPlayerExact(args[0]);
        if (targetPlayer == null) {
            String playerNotFoundMsg = ColorUtils.getConfigMessage("messages.error.playerNotFound", "&cPlayer '{playerName}' not found.")
                .replace("{playerName}", args[0]);
            inviter.sendMessage(playerNotFoundMsg);
            return true;
        }

        if (inviter.getUniqueId().equals(targetPlayer.getUniqueId())) {
            inviter.sendMessage(ColorUtils.getConfigMessage("messages.error.cannotInviteSelf", "&cYou cannot invite yourself."));
            return true;
        }

        if (clanManager.getClanByPlayer(targetPlayer.getUniqueId()) != null) {
            String targetInClanMsg = ColorUtils.getConfigMessage("messages.error.targetAlreadyInClan", "&c{targetPlayerName} is already in another clan.")
                .replace("{targetPlayerName}", targetPlayer.getName());
            inviter.sendMessage(targetInClanMsg);
            return true;
        }
        
        int maxMembers = ClanPlugin.getInstance().getConfig().getInt("clan.maxMembers", 20);
        if (inviterClan.getMembers().size() >= maxMembers) {
            String clanFullMsg = ColorUtils.getConfigMessage("messages.error.clanFull", "&cThe clan is full.")
                .replace("{maxMembers}", String.valueOf(maxMembers)); // Assuming {maxMembers} placeholder in message
            inviter.sendMessage(clanFullMsg);
            return true;
        }

        boolean success = clanManager.addInvite(targetPlayer.getUniqueId(), inviterClan.getName());
        if (!success) {
            String inviteFailMsg = ColorUtils.getConfigMessage("messages.error.inviteFail", "&cCould not send invitation. {targetPlayerName} might have a pending invite.")
                .replace("{targetPlayerName}", targetPlayer.getName());
            inviter.sendMessage(inviteFailMsg);
            
            String targetHasInviteMsg = ColorUtils.getRawConfigMessage("messages.error.targetHasInvite", "&e{inviterName} tried to invite you to {clanName}, but you already have a pending invite.")
                .replace("{inviterName}", inviter.getName())
                .replace("{clanName}", inviterClan.getName());
            targetPlayer.sendMessage(targetHasInviteMsg); // No prefix for target's message
            return true;
        }
        
        String inviteSentMsg = ColorUtils.getConfigMessage("messages.inviteSent", "&#00FFFFInvitation sent to {targetPlayerName}.")
            .replace("{targetPlayerName}", targetPlayer.getName());
        inviter.sendMessage(inviteSentMsg);
        
        String inviteReceivedMsg = ColorUtils.getRawConfigMessage("messages.inviteReceived", "&#00FFFFYou have received an invitation to join clan '&{clanName}&#00FFFF' from {inviterName}.")
            .replace("{clanName}", inviterClan.getName())
            .replace("{inviterName}", inviter.getName());
        targetPlayer.sendMessage(inviteReceivedMsg); // No prefix for target's message

        String howToAcceptMsg = ColorUtils.getRawConfigMessage("messages.howToAcceptInvite", "&bType &6/clanaccept {clanName} &bto accept or &6/clandecline {clanName} &bto decline.")
            .replace("{clanName}", inviterClan.getName());
        targetPlayer.sendMessage(howToAcceptMsg); // No prefix

        return true;
    }
}
