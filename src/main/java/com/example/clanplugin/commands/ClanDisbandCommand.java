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
import java.util.UUID; // Required for iterating members

public class ClanDisbandCommand implements CommandExecutor {

    private final ClanManager clanManager;

    public ClanDisbandCommand(ClanPlugin plugin) {
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

        if (!clan.isOwner(player.getUniqueId())) {
            player.sendMessage(ColorUtils.getConfigMessage("messages.error.notClanOwner", "&cOnly the clan owner can perform this action."));
            return true;
        }

        String clanName = clan.getName(); // Get name before it's deleted
        String disbandedByOwnerMsg = ColorUtils.getRawConfigMessage("messages.notify.clanDisbandedByOwner", "&cThe clan '{clanName}' has been disbanded by the owner.")
            .replace("{clanName}", clanName);

        for (UUID memberUUID : clan.getMembers()) {
            if (!memberUUID.equals(player.getUniqueId())) { 
                Player member = Bukkit.getPlayer(memberUUID);
                if (member != null && member.isOnline()) {
                    member.sendMessage(disbandedByOwnerMsg);
                }
            }
        }
        
        if (clanManager.deleteClan(clan.getName(), player.getUniqueId())) {
            String successMsg = ColorUtils.getConfigMessage("messages.clanDisbanded", "&#FF0000Your clan '&{clanName}&#FF0000' has been disbanded.")
                .replace("{clanName}", clanName);
            player.sendMessage(successMsg);
        } else {
            player.sendMessage(ColorUtils.getConfigMessage("messages.error.disbandFail", "&cFailed to disband the clan. Please try again."));
        }

        return true;
    }
}
