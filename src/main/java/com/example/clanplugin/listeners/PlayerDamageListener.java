package com.example.clanplugin.listeners;

import com.example.clanplugin.ClanPlugin;
import com.example.clanplugin.clans.Clan;
import com.example.clanplugin.clans.ClanManager;
import com.example.clanplugin.utils.ColorUtils; // For optional message
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class PlayerDamageListener implements Listener {
    private final ClanManager clanManager;
    private final ClanPlugin plugin; // To access config for the message

    public PlayerDamageListener(ClanPlugin plugin) {
        this.plugin = plugin;
        this.clanManager = ClanPlugin.getClanManager();
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return; // Only interested in Player vs Player
        }

        Player victim = (Player) event.getEntity();
        Player attacker = (Player) event.getDamager();

        Clan victimClan = clanManager.getClanByPlayer(victim.getUniqueId());
        Clan attackerClan = clanManager.getClanByPlayer(attacker.getUniqueId());

        // Check if both are in the same clan
        if (victimClan != null && victimClan.equals(attackerClan)) {
            if (!victimClan.isFriendlyFireAllowed()) {
                event.setCancelled(true);
                // Optionally send a message to the attacker, fetching from config.yml
                String pvpDisabledMsg = plugin.getConfig().getString("messages.error.pvpDisabledInternally");
                if (pvpDisabledMsg != null && !pvpDisabledMsg.isEmpty()) {
                    // We use getRawConfigMessage to avoid double prefixing if "messages.prefix" is already in "pvpDisabledInternally"
                    // Or, ensure pvpDisabledInternally does not have the main prefix in config.
                    // For simplicity, let's assume the message is defined raw or with its own prefix.
                    attacker.sendMessage(ColorUtils.translateColorCodes(ColorUtils.getConfigMessage("messages.error.pvpDisabledInternally", 
                        "&cFriendly fire is disabled in your clan.")));
                }
            }
        }
    }
}
