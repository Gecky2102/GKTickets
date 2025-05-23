package it.Gecky.gktickets.utils;

import it.Gecky.gktickets.GKTickets;
import org.bukkit.configuration.file.FileConfiguration;

public class ConfigManager {

    private final GKTickets plugin;
    private FileConfiguration config;

    public ConfigManager(GKTickets plugin) {
        this.plugin = plugin;
        this.loadConfig();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        this.config = plugin.getConfig();
    }

    /**
     * Ricarica la configurazione senza chiamare plugin.reloadConfig()
     * per evitare ricorsione infinita
     */
    public void reloadConfig() {
        // Don't call plugin.reloadConfig() here as it causes infinite recursion
        plugin.reloadConfigFile(); // Use our new method instead
        this.config = plugin.getConfig();
    }

    // Metodi per accedere alle configurazioni
    public String getDatabaseType() {
        return config.getString("database.type", "sqlite");
    }

    public String getDatabaseName() {
        return config.getString("database.name", "tickets");
    }
    
    public int getMaxTicketsPerUser() {
        return config.getInt("tickets.max-per-user", 3);
    }

    /**
     * Check if auto-close feature is enabled
     * @return true if enabled
     */
    public boolean isAutoCloseEnabled() {
        return config.getBoolean("tickets.auto-close.enabled", false);
    }

    /**
     * Get the auto-close time in hours
     * @return hours of inactivity before auto-close
     */
    public int getAutoCloseTime() {
        return config.getInt("tickets.auto-close.time", 72);
    }

    public boolean isNotifyStaffOnNewTicket() {
        return config.getBoolean("notifications.staff.new-ticket", true);
    }

    public boolean isNotifyStaffOnReply() {
        return config.getBoolean("notifications.staff.new-reply", true);
    }

    public boolean isNotifyPlayerOnReply() {
        return config.getBoolean("notifications.player.new-reply", true);
    }

    public boolean isNotifyPlayerOnClose() {
        return config.getBoolean("notifications.player.ticket-closed", true);
    }

    public boolean isSoundEnabled() {
        return config.getBoolean("notifications.sound.enabled", true);
    }

    public String getNewTicketSound() {
        return config.getString("notifications.sound.new-ticket", "entity.experience_orb.pickup");
    }

    public String getNewReplySound() {
        return config.getString("notifications.sound.new-reply", "entity.player.levelup");
    }

    /**
     * Checks if the feedback system is enabled
     * @return true if feedback system is enabled
     */
    public boolean isFeedbackEnabled() {
        return config.getBoolean("feedback.enabled", true);
    }
}
