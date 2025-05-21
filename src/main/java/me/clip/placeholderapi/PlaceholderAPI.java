package me.clip.placeholderapi;

import org.bukkit.entity.Player;

/**
 * A simple stub class for PlaceholderAPI to allow compilation
 * without requiring the actual dependency.
 * This will never be used at runtime because the plugin checks for
 * the presence of PlaceholderAPI before using it.
 */
public class PlaceholderAPI {
    
    /**
     * Stub method that simulates PlaceholderAPI functionality
     * for compilation purposes only.
     * 
     * @param player The player
     * @param text Text containing placeholders
     * @return The same text (placeholders not processed in stub)
     */
    public static String setPlaceholders(Player player, String text) {
        return text;
    }
}
