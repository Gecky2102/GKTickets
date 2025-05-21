package it.Gecky.gktickets.tasks;

import it.Gecky.gktickets.GKTickets;
import it.Gecky.gktickets.models.Ticket;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class AutoCloseTask extends BukkitRunnable {
    
    private final GKTickets plugin;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public AutoCloseTask(GKTickets plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void run() {
        // Skip if feature is disabled
        if (!plugin.getConfigManager().isAutoCloseEnabled()) {
            return;
        }
        
        // Get auto-close time in hours
        int hoursInactive = plugin.getConfigManager().getAutoCloseTime();
        if (hoursInactive <= 0) {
            return;
        }
        
        // Get all open tickets
        List<Ticket> openTickets = plugin.getDatabaseManager().getOpenTickets();
        int closedCount = 0;
        
        for (Ticket ticket : openTickets) {
            // Get the newest activity timestamp (either ticket creation or last reply)
            String lastActivityStr = ticket.getLastActivityTime();
            LocalDateTime lastActivity = LocalDateTime.parse(lastActivityStr, formatter);
            LocalDateTime now = LocalDateTime.now();
            
            // Calculate hours since last activity
            long hoursSinceActivity = ChronoUnit.HOURS.between(lastActivity, now);
            
            // Close ticket if inactive for too long
            if (hoursSinceActivity >= hoursInactive) {
                boolean closed = plugin.getDatabaseManager().closeTicket(ticket.getId(), "system",
                                 "Auto-closed due to " + hoursSinceActivity + " hours of inactivity");
                
                if (closed) {
                    closedCount++;
                    
                    // Notify the ticket owner if online
                    plugin.getNotificationManager().notifyAutoClosedTicket(ticket);
                    
                    // Log to Discord if enabled
                    if (plugin.getDiscordIntegration().isEnabled()) {
                        // Modify the sendTicketClosedMessage call to match the method signature
                        plugin.getDiscordIntegration().sendTicketClosedMessage(ticket, null);
                        
                        // If you need to include the automatic closure message, you might need to 
                        // update the DiscordIntegration class to support a different method signature
                    }
                }
            }
        }
        
        if (closedCount > 0) {
            plugin.getLogger().info("Auto-closed " + closedCount + " inactive tickets");
        }
    }
}
