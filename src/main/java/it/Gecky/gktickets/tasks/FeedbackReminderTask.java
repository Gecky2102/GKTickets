package it.Gecky.gktickets.tasks;

import it.Gecky.gktickets.GKTickets;
import it.Gecky.gktickets.models.Ticket;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Task per ricordare periodicamente agli utenti di dare feedback ai ticket chiusi
 */
public class FeedbackReminderTask extends BukkitRunnable {
    
    private final GKTickets plugin;
    
    public FeedbackReminderTask(GKTickets plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // Controlla solo per i giocatori che possono creare ticket
            if (player.hasPermission("gktickets.create")) {
                List<Ticket> pendingFeedback = plugin.getDatabaseManager().getPendingFeedbackTickets(player.getUniqueId());
                
                if (!pendingFeedback.isEmpty()) {
                    // Mostra il promemoria solo se ci sono ticket che necessitano di feedback
                    player.sendMessage(plugin.getMessageManager().getMessage("feedback-reminder-header"));
                    
                    TextComponent message = new TextComponent(plugin.getMessageManager().formatMessage("feedback-reminder-msg", 
                            Map.of("count", String.valueOf(pendingFeedback.size()))));
                            
                    // Se c'è solo un ticket che necessita di feedback, crea un link diretto
                    if (pendingFeedback.size() == 1) {
                        message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                                new ComponentBuilder("§7Clicca per dare feedback al ticket #" + pendingFeedback.get(0).getId()).create()));
                        message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, 
                                "/ticket feedback " + pendingFeedback.get(0).getId()));
                    } else {
                        // Altrimenti mostra un elenco di ticket
                        message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                                new ComponentBuilder("§7Clicca per vedere l'elenco dei tuoi ticket chiusi").create()));
                        message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, 
                                "/ticket list"));
                    }
                    
                    player.spigot().sendMessage(message);
                }
            }
        }
    }
}
