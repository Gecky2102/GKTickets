package it.Gecky.gktickets.categories;

import org.bukkit.ChatColor;

public class TicketCategory {
    
    private final String id;
    private final String name;
    private final String description;
    private final String color;
    private final String icon;
    private final boolean defaultPermission;
    
    public TicketCategory(String id, String name, String description, String color, String icon, boolean defaultPermission) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.color = color;
        this.icon = icon;
        this.defaultPermission = defaultPermission;
    }
    
    public String getId() {
        return id;
    }
    
    public String getName() {
        return name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getColor() {
        return color;
    }
    
    public String getIcon() {
        return icon;
    }
    
    public boolean isDefaultPermission() {
        return defaultPermission;
    }
    
    /**
     * Ottiene il nome della categoria formattato con colore
     * @return Nome formattato
     */
    public String getFormattedName() {
        return ChatColor.translateAlternateColorCodes('&', color) + name + ChatColor.RESET;
    }
    
    /**
     * Ottiene l'icona e il nome formattati
     * @return Icona e nome formattati
     */
    public String getFormattedNameWithIcon() {
        if (icon != null && !icon.isEmpty()) {
            return ChatColor.translateAlternateColorCodes('&', color) + icon + " " + name + ChatColor.RESET;
        } else {
            return getFormattedName();
        }
    }
}
