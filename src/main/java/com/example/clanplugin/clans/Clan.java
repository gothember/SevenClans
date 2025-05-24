package com.example.clanplugin.clans;

import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;

public class Clan {
    private String name;
    private UUID owner;
    private Set<UUID> members;
    private double balance;
    private String tag; // Optional: A short tag for the clan
    private boolean friendlyFireAllowed = false; // Added for PVP

    public Clan(String name, UUID owner) {
        this.name = name;
        this.owner = owner;
        this.members = new HashSet<>();
        this.members.add(owner); // Owner is implicitly a member
        this.balance = 0.0;
        this.tag = name.length() > 5 ? name.substring(0, 5) : name; // Default tag
    }

    // Getters
    public String getName() { return name; }
    public UUID getOwner() { return owner; }
    public Set<UUID> getMembers() { return members; }
    public double getBalance() { return balance; }
    public String getTag() { return tag; }

    // Setters
    public void setTag(String tag) { this.tag = tag; }
    public void setBalance(double balance) { this.balance = balance; }

    // Member management
    public boolean addMember(UUID memberUuid) {
        return members.add(memberUuid);
    }

    public boolean removeMember(UUID memberUuid) {
        // Prevent removing the owner; disband should be used.
        if (owner.equals(memberUuid)) {
            return false; 
        }
        return members.remove(memberUuid);
    }

    public boolean isMember(UUID memberUuid) {
        return members.contains(memberUuid);
    }

    public boolean isOwner(UUID memberUuid) {
        return owner.equals(memberUuid);
    }
    
    // For saving to YAML, we might need methods to serialize members and owner
    public List<String> getMemberUUIDsAsString() {
        List<String> stringUUIDs = new ArrayList<>();
        for (UUID uuid : members) {
            stringUUIDs.add(uuid.toString());
        }
        return stringUUIDs;
    }

    public String getOwnerUUIDAsString() {
        return owner.toString();
    }

    // Getter and Setter for friendlyFireAllowed
    public boolean isFriendlyFireAllowed() {
        return friendlyFireAllowed;
    }

    public void setFriendlyFireAllowed(boolean friendlyFireAllowed) {
        this.friendlyFireAllowed = friendlyFireAllowed;
    }
}
