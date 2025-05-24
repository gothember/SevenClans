package com.example.clanplugin.utils;

import net.md_5.bungee.api.ChatColor; // Required for ChatColor.of for HEX
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtils {

    // Pattern to match &#RRGGBB format
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    /**
     * Translates a string containing &#RRGGBB HEX color codes and traditional & color codes
     * into a string with Bukkit ChatColors.
     *
     * @param textToTranslate The string to translate.
     * @return The translated string with colors.
     */
    public static String translateColorCodes(String textToTranslate) {
        if (textToTranslate == null || textToTranslate.isEmpty()) {
            return "";
        }

        Matcher matcher = HEX_PATTERN.matcher(textToTranslate);
        StringBuffer buffer = new StringBuffer(textToTranslate.length() + 4 * 8); // Preallocate buffer

        while (matcher.find()) {
            String group = matcher.group(1);
            // Replace &#RRGGBB with ChatColor.of("#RRGGBB")
            matcher.appendReplacement(buffer, ChatColor.of("#" + group).toString());
        }
        matcher.appendTail(buffer);

        // Then translate traditional & codes
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }
    
    /**
     * Helper method to get a prefixed and colored message from config.
     * This assumes messages in config are plain strings that need color translation.
     *
     * @param path The path to the message in config.yml
     * @param defaultMessage The default message if path is not found.
     * @return Translated and prefixed message.
     */
    public static String getConfigMessage(String path, String defaultMessage) {
        String prefix = com.example.clanplugin.ClanPlugin.getInstance().getConfig().getString("messages.prefix", "&#008080[ClanPlugin] &r");
        String message = com.example.clanplugin.ClanPlugin.getInstance().getConfig().getString(path, defaultMessage);
        return translateColorCodes(prefix + message);
    }

    /**
     * Helper method to get a colored message (without plugin prefix) from config.
     *
     * @param path The path to the message in config.yml
     * @param defaultMessage The default message if path is not found.
     * @return Translated message.
     */
    public static String getRawConfigMessage(String path, String defaultMessage) {
        String message = com.example.clanplugin.ClanPlugin.getInstance().getConfig().getString(path, defaultMessage);
        return translateColorCodes(message);
    }
}
