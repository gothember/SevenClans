package com.example.clanplugin.commands;

import com.example.clanplugin.ClanPlugin;
import com.example.clanplugin.clans.Clan;
import com.example.clanplugin.clans.ClanManager;
import com.example.clanplugin.utils.ColorUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
// import java.util.UUID; // For announcing to members, if implemented

public class ClanPvpCommand implements CommandExecutor {
    private final ClanManager clanManager;

    public ClanPvpCommand(ClanPlugin plugin) {
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

        // For now, only owner. Later, expand to officers.
        if (!clan.isOwner(player.getUniqueId())) {
            player.sendMessage(ColorUtils.getConfigMessage("messages.error.notClanOwner", "&cOnly the clan owner can perform this action."));
            return true;
        }

        boolean currentStatus = clan.isFriendlyFireAllowed();
        clan.setFriendlyFireAllowed(!currentStatus); // Toggle
        clanManager.saveClans(); // Persist change

        String status = clan.isFriendlyFireAllowed() ? 
                        ColorUtils.getRawConfigMessage("messages.statusWords.enabled", "ENABLED") : 
                        ColorUtils.getRawConfigMessage("messages.statusWords.disabled", "DISABLED");
        
        String message = ColorUtils.getConfigMessage("messages.clanPvpStatus", "&aClan friendly fire is now {status}.")
                                   .replace("{status}", status);
        player.sendMessage(message);
        
        // Announce to clan members if desired
        // String announceMsg = ColorUtils.getRawConfigMessage("messages.notify.clanPvpStatusChanged", "&bClan friendly fire status changed to {status} by {playerName}.")
        //                            .replace("{status}", status)
        //                            .replace("{playerName}", player.getName());
        // for (UUID memberUUID : clan.getMembers()) {
        //    if (!memberUUID.equals(player.getUniqueId())) { // Don't re-notify the player who changed it
        //        Player member = Bukkit.getPlayer(memberUUID);
        //        if (member != null && member.isOnline()) {
        //            member.sendMessage(announceMsg);
        //        }
        //    }
        // }
        return true;
    }
}
