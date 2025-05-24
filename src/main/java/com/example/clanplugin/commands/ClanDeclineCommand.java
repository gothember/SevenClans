package com.example.clanplugin.commands;

import com.example.clanplugin.ClanPlugin;
import com.example.clanplugin.clans.Clan;
import com.example.clanplugin.clans.ClanManager;
import com.example.clanplugin.utils.ColorUtils; // Import ColorUtils
import org.bukkit.Bukkit;
// import org.bukkit.ChatColor; // No longer needed
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
            sender.sendMessage(ColorUtils.getRawConfigMessage("messages.error.playerOnlyCommand", "&cThis command can only be run by a player."));
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            player.sendMessage(ColorUtils.translateColorCodes("&cUsage: /clandecline <clanName>")); // Or get from plugin.yml
            return false; 
        }

        String clanNameToDecline = args[0];
        String pendingInviteClanName = clanManager.getInvite(player.getUniqueId());

        if (pendingInviteClanName == null) {
            player.sendMessage(ColorUtils.getConfigMessage("messages.error.noPendingInvite", "&cYou do not have any pending clan invitations."));
            return true;
        }

        if (!pendingInviteClanName.equalsIgnoreCase(clanNameToDecline)) {
            String wrongInviteMsg = ColorUtils.getConfigMessage("messages.error.notInvitedToThisClan", "&cYou do not have an invitation from '{clanName}'. Your invite is from '{pendingClanName}'.")
                .replace("{clanName}", clanNameToDecline)
                .replace("{pendingClanName}", pendingInviteClanName);
            player.sendMessage(wrongInviteMsg);
            player.sendMessage(ColorUtils.getRawConfigMessage("messages.howToDeclineSpecific", "&cType &6/clandecline {pendingClanName} &cto decline it.").replace("{pendingClanName}", pendingInviteClanName));
            return true;
        }

        Clan clan = clanManager.getClan(pendingInviteClanName); // Get clan to notify owner

        if (clanManager.removeInvite(player.getUniqueId())) {
            String declinedMsg = ColorUtils.getConfigMessage("messages.inviteDeclined", "&aYou have declined the invitation from clan '{clanName}'.")
                .replace("{clanName}", pendingInviteClanName);
            player.sendMessage(declinedMsg);

            if (clan != null) {
                Player clanOwner = Bukkit.getPlayer(clan.getOwner());
                if (clanOwner != null && clanOwner.isOnline()) {
                    String ownerNotifyDecline = ColorUtils.getRawConfigMessage("messages.notify.ownerInviteDeclined", "&e{playerName} has declined your invitation to join {clanName}.")
                        .replace("{playerName}", player.getName())
                        .replace("{clanName}", clan.getName());
                    clanOwner.sendMessage(ownerNotifyDecline);
                }
            }
        } else {
            player.sendMessage(ColorUtils.getConfigMessage("messages.error.declineFail", "&cFailed to decline invitation. It might have already been rescinded or expired."));
        }

        return true;
    }
}
