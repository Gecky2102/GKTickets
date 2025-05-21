package it.Gecky.gktickets.tasks;

import it.Gecky.gktickets.GKTickets;
import it.Gecky.gktickets.models.Ticket;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Task che periodicamente ricorda ai giocatori di dare feedback ai ticket chiusi
 */
public class FeedbackReminderTask extends BukkitRunnable {
    
    private final GKTickets plugin;
    
    public FeedbackReminderTask(GKTickets plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void run() {
        // Salta se i promemoria per il feedback sono disabilitati
        int reminderInterval = plugin.getConfig().getInt("feedback.reminder-interval", 0);
        if (reminderInterval <= 0 || !plugin.getConfig().getBoolean("feedback.enabled", true)) {
            return;
        }
        
        // Controlla i giocatori online che hanno ticket che necessitano di feedback
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            // Ottieni i ticket che necessitano di feedback
            List<Ticket> pendingFeedback = plugin.getDatabaseManager().getPendingFeedbackTickets(player.getUniqueId());
            
            if (!pendingFeedback.isEmpty()) {
                player.sendMessage(plugin.getMessageManager().getMessage("feedback-reminder-header"));
                
                // Crea un messaggio cliccabile
                TextComponent message = new TextComponent(
                    plugin.getMessageManager().formatMessage("feedback-reminder-msg", 
                    Map.of("count", String.valueOf(pendingFeedback.size())))
                );
                
                // Aggiungi eventi di clic e hover
                message.setClickEvent(new ClickEvent(
                    ClickEvent.Action.RUN_COMMAND, 
                    "/ticket feedback " + pendingFeedback.get(0).getId()
                ));
                
                message.setHoverEvent(new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT, 
                    new ComponentBuilder("§7Clicca per dare un feedback").create()
                ));
                
                player.spigot().sendMessage(message);
            }
        }
    }
}
