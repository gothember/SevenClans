package com.example.clanplugin.clans;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.Bukkit; // For logging

public class ClanManager {
    private final JavaPlugin plugin;
    private final Map<String, Clan> clans = new HashMap<>(); // Clan name to Clan object
    private final Map<UUID, Clan> playerClanMap = new HashMap<>(); // Player UUID to their Clan object
    private File clansFile;
    private FileConfiguration clansConfig;

    public ClanManager(JavaPlugin plugin) {
        this.plugin = plugin;
        clansFile = new File(plugin.getDataFolder(), "clans.yml");
        if (!clansFile.exists()) {
            plugin.saveResource("clans.yml", false); // Save default clans.yml if not exists (can be empty)
        }
        clansConfig = YamlConfiguration.loadConfiguration(clansFile);
        loadClans();
    }

    public Clan createClan(String name, UUID owner) {
        if (clans.containsKey(name.toLowerCase())) {
            return null; // Clan already exists
        }
        if (getClanByPlayer(owner) != null) {
            return null; // Player already in a clan
        }
        Clan clan = new Clan(name, owner);
        clans.put(name.toLowerCase(), clan);
        playerClanMap.put(owner, clan);
        saveClans();
        return clan;
    }

    public boolean deleteClan(String name, UUID requester) {
        Clan clan = getClan(name);
        if (clan == null || !clan.isOwner(requester)) {
            return false; // Clan doesn't exist or requester is not owner
        }
        // Remove all members from playerClanMap
        for (UUID member : clan.getMembers()) {
            playerClanMap.remove(member);
        }
        clans.remove(name.toLowerCase());
        saveClans();
        return true;
    }
    
    public Clan getClan(String name) {
        return clans.get(name.toLowerCase());
    }

    public Clan getClanByPlayer(UUID playerUuid) {
        return playerClanMap.get(playerUuid);
    }
    
    public boolean addPlayerToClan(Clan clan, UUID playerUuid) {
        if (clan == null || getClanByPlayer(playerUuid) != null) {
            return false; // Clan doesn't exist or player already in a clan
        }
        if (clan.addMember(playerUuid)) {
            playerClanMap.put(playerUuid, clan);
            saveClans();
            return true;
        }
        return false;
    }

    public boolean removePlayerFromClan(Clan clan, UUID playerUuid) {
        if (clan == null || clan.isOwner(playerUuid)) {
            return false; // Cannot remove owner this way
        }
        if (clan.removeMember(playerUuid)) {
            playerClanMap.remove(playerUuid);
            saveClans();
            return true;
        }
        return false;
    }

    public void loadClans() {
        clans.clear();
        playerClanMap.clear();
        if (clansConfig.getConfigurationSection("clans") == null) {
            return;
        }
        for (String clanName : clansConfig.getConfigurationSection("clans").getKeys(false)) {
            String path = "clans." + clanName;
            UUID owner = UUID.fromString(clansConfig.getString(path + ".owner"));
            Clan clan = new Clan(clanName, owner); // Clan name from key, not stored name field
            clan.setBalance(clansConfig.getDouble(path + ".balance", 0.0));
            clan.setTag(clansConfig.getString(path + ".tag", clanName.length() > 5 ? clanName.substring(0,5) : clanName));
            clan.setFriendlyFireAllowed(clansConfig.getBoolean(path + ".friendlyFireAllowed", false)); // Added
            
            List<String> memberUUIDs = clansConfig.getStringList(path + ".members");
            for (String uuidStr : memberUUIDs) {
                UUID memberUUID = UUID.fromString(uuidStr);
                if (!memberUUID.equals(owner)) { // Owner already added in constructor
                    clan.addMember(memberUUID);
                }
            }
            
            clans.put(clanName.toLowerCase(), clan);
            for (UUID member : clan.getMembers()) {
                playerClanMap.put(member, clan);
            }
        }
        plugin.getLogger().info("Loaded " + clans.size() + " clans.");
    }

    public void saveClans() {
        clansConfig.set("clans", null); // Clear existing clans section
        for (Map.Entry<String, Clan> entry : clans.entrySet()) {
            String clanNameKey = entry.getKey(); // Use the lowercase name as key for consistency
            Clan clan = entry.getValue();
            String path = "clans." + clanNameKey;
            clansConfig.set(path + ".name", clan.getName()); // Store original casing name
            clansConfig.set(path + ".owner", clan.getOwnerUUIDAsString());
            clansConfig.set(path + ".members", clan.getMemberUUIDsAsString());
            clansConfig.set(path + ".balance", clan.getBalance());
            clansConfig.set(path + ".tag", clan.getTag());
            clansConfig.set(path + ".friendlyFireAllowed", clan.isFriendlyFireAllowed()); // Added
        }
        try {
            clansConfig.save(clansFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save clans to clans.yml: " + e.getMessage());
        }
    }
    
    public Map<String, Clan> getAllClans() {
        return new HashMap<>(clans); // Return a copy
    }

    // Add near other map declarations
    private final Map<UUID, String> pendingInvites = new HashMap<>(); // Invited Player UUID -> Clan Name
    private final Map<UUID, Long> inviteTimestamps = new HashMap<>(); // Invited Player UUID -> Timestamp of invite

    // Method to add an invite
    // Returns false if player already has an invite or other issues
    public boolean addInvite(UUID invitedPlayerUUID, String clanName) {
        if (pendingInvites.containsKey(invitedPlayerUUID)) {
            // Player already has an invite, maybe from another clan.
            // For simplicity, only one invite at a time.
            return false; 
        }
        Clan clan = getClan(clanName);
        if (clan == null) return false; // Should not happen if called correctly

        pendingInvites.put(invitedPlayerUUID, clan.getName());
        inviteTimestamps.put(invitedPlayerUUID, System.currentTimeMillis()); // Store timestamp
        plugin.getLogger().info("Invitation created for " + invitedPlayerUUID + " to join " + clanName);
        return true;
    }

    // Method to get a pending invite for a player
    public String getInvite(UUID invitedPlayerUUID) {
        long expireAfterMillis = com.example.clanplugin.ClanPlugin.getInstance().getConfig().getLong("invitations.expireAfterSeconds", 300) * 1000;
        
        if (expireAfterMillis > 0) { // 0 means never expires
            Long creationTime = inviteTimestamps.get(invitedPlayerUUID);
            if (creationTime != null && (System.currentTimeMillis() - creationTime > expireAfterMillis)) {
                String clanName = pendingInvites.get(invitedPlayerUUID); // Get clan name for logging/notification before removing
                removeInvite(invitedPlayerUUID); // This removes from both maps
                plugin.getLogger().info("Expired invite for " + invitedPlayerUUID + " to clan " + clanName + " was accessed and removed.");
                // Notifications for this specific access case can be added here or rely on checkExpiredInvites
                return null; // Invite is expired
            }
        }
        return pendingInvites.get(invitedPlayerUUID);
    }

    // Method to remove an invite (after accept/decline or expiration)
    public boolean removeInvite(UUID invitedPlayerUUID) {
        inviteTimestamps.remove(invitedPlayerUUID); // Remove timestamp
        String clanName = pendingInvites.remove(invitedPlayerUUID);
        if (clanName != null) {
            plugin.getLogger().info("Invitation removed for " + invitedPlayerUUID + " from clan " + clanName);
            return true;
        }
        return false;
    }
    
    // Method to periodically check and clean up expired invites
    public void checkExpiredInvites() {
        long expireAfterMillis = com.example.clanplugin.ClanPlugin.getInstance().getConfig().getLong("invitations.expireAfterSeconds", 300) * 1000;
        if (expireAfterMillis <= 0) { // Expiry is disabled
            return;
        }

        // Iterate over a copy of keys to avoid ConcurrentModificationException while removing
        for (UUID invitedPlayerUUID : new java.util.HashSet<>(pendingInvites.keySet())) { 
            Long creationTime = inviteTimestamps.get(invitedPlayerUUID);
            if (creationTime == null) { // Should not happen if data is consistent
                pendingInvites.remove(invitedPlayerUUID); // Clean up inconsistent entry
                continue;
            }

            if ((System.currentTimeMillis() - creationTime) > expireAfterMillis) {
                String clanName = pendingInvites.get(invitedPlayerUUID); // Get clan name before removing invite
                removeInvite(invitedPlayerUUID); // This removes from both maps

                org.bukkit.entity.Player invitedPlayer = Bukkit.getPlayer(invitedPlayerUUID);
                if (invitedPlayer != null && invitedPlayer.isOnline()) {
                    String msg = com.example.clanplugin.utils.ColorUtils.getConfigMessage("messages.inviteExpiredForInvited", "&cYour clan invitation from '{clanName}' has expired.")
                                           .replace("{clanName}", clanName == null ? "a clan" : clanName);
                    invitedPlayer.sendMessage(msg);
                }

                if (clanName != null) {
                    Clan clan = getClan(clanName);
                    if (clan != null) {
                        org.bukkit.entity.Player clanOwner = Bukkit.getPlayer(clan.getOwner());
                        if (clanOwner != null && clanOwner.isOnline()) {
                            org.bukkit.OfflinePlayer offlineInvitedPlayer = Bukkit.getOfflinePlayer(invitedPlayerUUID);
                            String invitedPlayerName = offlineInvitedPlayer.getName() != null ? offlineInvitedPlayer.getName() : invitedPlayerUUID.toString();
                            
                            String msg = com.example.clanplugin.utils.ColorUtils.getConfigMessage("messages.inviteExpiredForInviter", "&eThe invitation sent to '{playerName}' for your clan '{clanName}' has expired.")
                                                   .replace("{playerName}", invitedPlayerName)
                                                   .replace("{clanName}", clanName);
                            clanOwner.sendMessage(msg);
                        }
                    }
                }
                plugin.getLogger().info("Expired and removed clan invitation for player " + invitedPlayerUUID + " from clan " + clanName);
            }
        }
    }
        
    // Modify addPlayerToClan to use/remove invites
    // public boolean addPlayerToClan(Clan clan, UUID playerUuid) { // Old signature, if it was public before
    public boolean addPlayerToClan(Clan clan, UUID playerUuid) { // Assuming it's part of an interface or needs to be public
        if (clan == null || getClanByPlayer(playerUuid) != null) {
            return false; // Clan doesn't exist or player already in a clan
        }
        // Ensure they were invited (or handle cases where invite isn't strictly necessary, e.g. admin command)
        // This check is more relevant for the /clan accept command.
        // For direct additions (e.g. admin command), an invite might not be required.
        // However, if this method is *only* called after an invite is accepted,
        // then the invite has already been consumed by the accept command.

        // String invitedToClanName = getInvite(playerUuid); // This would be for checking if an invite *exists*
        // if (invitedToClanName == null || !invitedToClanName.equalsIgnoreCase(clan.getName())) {
        // return false; // Not invited or invited to a different clan
        // }

        if (clan.addMember(playerUuid)) {
            playerClanMap.put(playerUuid, clan);
            // The invite should ideally be removed by the command that processes the acceptance (e.g. /clanaccept)
            // before calling this method. If addPlayerToClan is a general method,
            // then removing invite here is fine as a cleanup.
            removeInvite(playerUuid); 
            saveClans();
            return true;
        }
        return false;
    }
}
