package com.example.clanplugin.commands;

import com.example.clanplugin.ClanPlugin;
import com.example.clanplugin.clans.Clan;
import com.example.clanplugin.clans.ClanManager;
import com.example.clanplugin.utils.ColorUtils; // Import ColorUtils
// import org.bukkit.ChatColor; // No longer needed
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
            sender.sendMessage(ColorUtils.getRawConfigMessage("messages.error.playerOnlyCommand", "&cThis command can only be run by a player."));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(ColorUtils.translateColorCodes("&cUsage: /clanaccept <clanName>")); // Or get from plugin.yml
            return false; 
        }

        String clanNameToAccept = args[0];
        String pendingInviteClanName = clanManager.getInvite(player.getUniqueId());

        if (pendingInviteClanName == null) {
            player.sendMessage(ColorUtils.getConfigMessage("messages.error.noPendingInvite", "&cYou do not have any pending clan invitations."));
            return true;
        }

        if (!pendingInviteClanName.equalsIgnoreCase(clanNameToAccept)) {
            String wrongInviteMsg = ColorUtils.getConfigMessage("messages.error.notInvitedToThisClan", "&cYou do not have an invitation from '{clanName}'. Your invite is from '{pendingClanName}'.")
                .replace("{clanName}", clanNameToAccept)
                .replace("{pendingClanName}", pendingInviteClanName);
            player.sendMessage(wrongInviteMsg);
            player.sendMessage(ColorUtils.getRawConfigMessage("messages.howToAcceptSpecific", "&cType &6/clanaccept {pendingClanName} &cto accept it.").replace("{pendingClanName}", pendingInviteClanName));
            return true;
        }

        Clan clan = clanManager.getClan(pendingInviteClanName);
        if (clan == null) {
            String clanNoLongerExistsMsg = ColorUtils.getConfigMessage("messages.error.clanNoLongerExists", "&cThe clan '{clanName}' no longer exists.")
                .replace("{clanName}", pendingInviteClanName);
            player.sendMessage(clanNoLongerExistsMsg);
            clanManager.removeInvite(player.getUniqueId()); 
            return true;
        }
        
        if (clanManager.getClanByPlayer(player.getUniqueId()) != null) {
            player.sendMessage(ColorUtils.getConfigMessage("messages.error.alreadyInClan", "&cYou are already in a clan. Please leave your current clan first."));
            return true;
        }

        int maxMembers = plugin.getConfig().getInt("clan.maxMembers", 20);
        if (clan.getMembers().size() >= maxMembers) {
            String clanFullMsg = ColorUtils.getConfigMessage("messages.error.clanFull", "&cThe clan '{clanName}' is full ({maxMembers} members).")
                .replace("{clanName}", clan.getName())
                .replace("{maxMembers}", String.valueOf(maxMembers));
            player.sendMessage(clanFullMsg);
            
            Player clanOwner = Bukkit.getPlayer(clan.getOwner());
            if(clanOwner != null && clanOwner.isOnline()){
                String ownerNotifyFull = ColorUtils.getRawConfigMessage("messages.notify.ownerClanFullOnAccept", "&e{playerName} tried to accept your invite, but your clan is full.")
                    .replace("{playerName}", player.getName());
                clanOwner.sendMessage(ownerNotifyFull);
            }
            clanManager.removeInvite(player.getUniqueId());
            return true;
        }

        if (clanManager.addPlayerToClan(clan, player.getUniqueId())) {
            String joinedMsg = ColorUtils.getConfigMessage("messages.clanJoined", "&aYou have successfully joined clan: {clanName}!")
                .replace("{clanName}", clan.getName());
            player.sendMessage(joinedMsg);

            Player clanOwner = Bukkit.getPlayer(clan.getOwner());
            if (clanOwner != null && clanOwner.isOnline()) {
                String ownerNotifyJoin = ColorUtils.getRawConfigMessage("messages.notify.ownerPlayerJoined", "&a{playerName} has accepted the invitation and joined your clan!")
                    .replace("{playerName}", player.getName());
                clanOwner.sendMessage(ownerNotifyJoin);
            }
            
            String memberNotifyJoin = ColorUtils.getRawConfigMessage("messages.notify.membersPlayerJoined", "&b{playerName} has joined the clan!")
                .replace("{playerName}", player.getName());
            for (UUID memberUUID : clan.getMembers()) {
                if (!memberUUID.equals(player.getUniqueId()) && !memberUUID.equals(clan.getOwner())) { 
                    Player member = Bukkit.getPlayer(memberUUID);
                    if (member != null && member.isOnline()) {
                        member.sendMessage(memberNotifyJoin);
                    }
                }
            }
        } else {
            player.sendMessage(ColorUtils.getConfigMessage("messages.error.joinFail", "&cFailed to join the clan. Please try again."));
        }
        return true;
    }
}
