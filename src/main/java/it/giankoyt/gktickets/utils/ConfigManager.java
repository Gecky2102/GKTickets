package it.giankoyt.gktickets.utils;

import it.giankoyt.gktickets.GKTickets;
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

    public void reloadConfig() {
        plugin.reloadConfig();
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

    public boolean isAutoCloseEnabled() {
        return config.getBoolean("tickets.auto-close.enabled", false);
    }

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
}
