package com.example.clanplugin.commands;

import com.example.clanplugin.ClanPlugin;
import com.example.clanplugin.clans.Clan;
import com.example.clanplugin.clans.ClanManager;
import com.example.clanplugin.utils.ColorUtils; // Import ColorUtils
import org.bukkit.Bukkit;
// import org.bukkit.ChatColor; // No longer needed for direct message construction
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.List;
import java.util.ArrayList;

public class ClanMembersCommand implements CommandExecutor {

    private final ClanManager clanManager;

    public ClanMembersCommand(ClanPlugin plugin) {
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

        List<String> memberNameList = new ArrayList<>();
        for (UUID memberUUID : clan.getMembers()) {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(memberUUID);
            String name = offlinePlayer.getName();
            if (name == null) {
                // This case is rare, but good to handle. Could be a player who never joined or data corruption.
                name = "&7Unknown (UUID: " + memberUUID.toString() + ")"; 
            }

            String status;
            if (clan.isOwner(memberUUID)) {
                status = ColorUtils.getRawConfigMessage("messages.clanMemberStatus.owner", "&6 (Owner)");
            } else if (offlinePlayer.isOnline()) {
                status = ColorUtils.getRawConfigMessage("messages.clanMemberStatus.online", "&a (Online)");
            } else {
                status = ColorUtils.getRawConfigMessage("messages.clanMemberStatus.offline", "&7 (Offline)");
            }
            // Name itself should not be translated if it's a player name.
            // But the status string from config *is* translated.
            memberNameList.add(name + status); 
        }
        
        String headerMsg = ColorUtils.getConfigMessage("messages.clanMembersHeader", "&bMembers of {clanName} ({currentMembers}/{maxMembers}):")
            .replace("{clanName}", clan.getName())
            .replace("{currentMembers}", String.valueOf(clan.getMembers().size()))
            .replace("{maxMembers}", String.valueOf(ClanPlugin.getInstance().getConfig().getInt("clan.maxMembers", 20)));
        player.sendMessage(headerMsg);

        if (memberNameList.isEmpty()) {
            player.sendMessage(ColorUtils.getConfigMessage("messages.noClanMembers", "&7- No other members -"));
        } else {
            String memberListItemFormat = ColorUtils.getRawConfigMessage("messages.clanMemberListItem", "&f- {memberName}");
            for (String memberNameAndStatus : memberNameList) {
                // Apply ColorUtils.translateColorCodes to the combined string
                // if player names could have color codes and status has color codes
                player.sendMessage(ColorUtils.translateColorCodes(memberListItemFormat.replace("{memberName}", memberNameAndStatus)));
            }
        }
        return true;
    }
}
