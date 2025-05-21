package it.Gecky.gktickets.reporting;

import it.Gecky.gktickets.GKTickets;
import it.Gecky.gktickets.models.Ticket;
import it.Gecky.gktickets.models.Reply;

import javax.net.ssl.HttpsURLConnection;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.json.simple.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class ReportManager {
    
    private final GKTickets plugin;
    private final boolean enabled;
    private final boolean reportBlacklist;
    private final boolean reportTicketStats;
    private final String webhookUrl;
    private final String username;
    private final String avatarUrl;
    private final int blacklistColor;
    private final int statsColor;
    
    public ReportManager(GKTickets plugin) {
        this.plugin = plugin;
        
        FileConfiguration config = plugin.getConfig();
        this.enabled = config.getBoolean("reporting.enabled", false);
        this.reportBlacklist = config.getBoolean("reporting.report-types.blacklist", true);
        this.reportTicketStats = config.getBoolean("reporting.report-types.ticket-stats", true);
        this.webhookUrl = config.getString("reporting.webhook-url", "");
        this.username = config.getString("reporting.username", "GKTickets Reporter");
        this.avatarUrl = config.getString("reporting.avatar-url", "");
        
        // Parse colors from hex strings
        String blacklistColorStr = config.getString("reporting.colors.blacklist", "#FF0000");
        String statsColorStr = config.getString("reporting.colors.stats", "#00FFFF");
        
        // Convert hex colors to decimal
        this.blacklistColor = Integer.parseInt(blacklistColorStr.replace("#", ""), 16);
        this.statsColor = Integer.parseInt(statsColorStr.replace("#", ""), 16);
        
        // Schedule periodic stats reporting
        if (this.enabled && this.reportTicketStats) {
            int interval = config.getInt("reporting.stats-interval", 24); // Hours
            scheduleStatsReporting(interval);
        }
    }
    
    /**
     * Schedule periodic stats reporting
     * @param intervalHours Hours between reports
     */
    private void scheduleStatsReporting(int intervalHours) {
        if (intervalHours <= 0) return;
        
        // Convert to ticks (20 ticks per second * 60 seconds * 60 minutes * hours)
        long ticks = 20L * 60L * 60L * intervalHours;
        
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, this::reportTicketStats, ticks, ticks);
        plugin.getLogger().info("Scheduled ticket stats reporting every " + intervalHours + " hours");
    }
    
    /**
     * Report a player being blacklisted
     * @param playerName Player's name
     * @param reason Reason for blacklisting
     * @param staffName Staff who blacklisted
     * @param expiryDays Days until expiry (0 for permanent)
     */
    public void reportBlacklist(String playerName, String reason, String staffName, int expiryDays) {
        if (!enabled || !reportBlacklist || webhookUrl.isEmpty()) {
            return;
        }
        
        String title = "Player Blacklisted from Tickets";
        String description = "**Player**: " + playerName + "\n" +
                            "**Reason**: " + reason + "\n" +
                            "**Admin**: " + staffName + "\n" +
                            "**Duration**: " + (expiryDays <= 0 ? "Permanent" : expiryDays + " days") + "\n" +
                            "**Date**: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        sendDiscordWebhook(title, description, blacklistColor);
    }
    
    /**
     * Report a player being unblacklisted
     * @param playerName Player's name
     */
    public void reportUnblacklist(String playerName) {
        if (!enabled || !reportBlacklist || webhookUrl.isEmpty()) {
            return;
        }
        
        String title = "Player Removed from Blacklist";
        String description = "**Player**: " + playerName + "\n" +
                            "**Date**: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        sendDiscordWebhook(title, description, blacklistColor);
    }
    
    /**
     * Report ticket statistics
     */
    @SuppressWarnings("unchecked")
    public void reportTicketStats() {
        if (!enabled || !reportTicketStats || webhookUrl.isEmpty()) {
            return;
        }
        
        Map<String, Object> stats = plugin.getDatabaseManager().getTicketStats();
        
        int totalTickets = Integer.parseInt(String.valueOf(stats.getOrDefault("total", 0)));
        int openTickets = Integer.parseInt(String.valueOf(stats.getOrDefault("open", 0)));
        int closedTickets = Integer.parseInt(String.valueOf(stats.getOrDefault("closed", 0)));
        String avgRating = (String) stats.getOrDefault("avg_rating", "N/A");
        
        String title = "Ticket System Statistics Report";
        String description = "**Total Tickets**: " + totalTickets + "\n" +
                            "**Open Tickets**: " + openTickets + "\n" +
                            "**Closed Tickets**: " + closedTickets + "\n" +
                            "**Average Rating**: " + avgRating + " ⭐\n" +
                            "**Date**: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        sendDiscordWebhook(title, description, statsColor);
    }
    
    /**
     * Send a message to Discord webhook
     * @param title Title of the embed
     * @param description Description text
     * @param color Color of the embed
     */
    @SuppressWarnings("unchecked")
    private void sendDiscordWebhook(String title, String description, int color) {
        if (webhookUrl.isEmpty()) return;
        
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL url = new URL(webhookUrl);
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                
                // Create JSON payload
                JSONObject embed = new JSONObject();
                embed.put("title", title);
                embed.put("description", description);
                embed.put("color", color);
                
                List<JSONObject> embedsList = new ArrayList<>();
                embedsList.add(embed);
                
                JSONObject payload = new JSONObject();
                payload.put("embeds", embedsList);
                
                if (!username.isEmpty()) {
                    payload.put("username", username);
                }
                
                if (!avatarUrl.isEmpty()) {
                    payload.put("avatar_url", avatarUrl);
                }
                
                // Write payload
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = payload.toJSONString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                
                // Get response
                int responseCode = connection.getResponseCode();
                if (responseCode != 204) {
                    plugin.getLogger().warning("Discord webhook returned code: " + responseCode);
                }
                
                connection.disconnect();
            } catch (Exception e) {
                plugin.getLogger().severe("Error sending Discord webhook: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Check if reporting is enabled
     * @return true if enabled
     */
    public boolean isEnabled() {
        return enabled;
    }
}
