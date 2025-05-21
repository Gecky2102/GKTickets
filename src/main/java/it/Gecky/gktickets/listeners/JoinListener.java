package it.Gecky.gktickets.listeners;

import it.Gecky.gktickets.GKTickets;
import it.Gecky.gktickets.models.Ticket;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;

public class JoinListener implements Listener {
    
    private final GKTickets plugin;
    
    public JoinListener(GKTickets plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        // Esegui le verifiche dopo un breve ritardo per non rallentare il login
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!player.isOnline()) return;
                
                // Per i giocatori, controlla le risposte non lette
                if (player.hasPermission("gktickets.create")) {
                    checkUnreadReplies(player);
                    
                    // Controlla se ci sono ticket che necessitano di feedback
                    checkPendingFeedback(player);
                }
                
                // Per lo staff, controlla i ticket che necessitano di risposta
                if (player.hasPermission("gktickets.staff")) {
                    checkPendingTickets(player);
                }
            }
        }.runTaskLater(plugin, 40); // Circa 2 secondi dopo il login
    }
    
    /**
     * Controlla se il giocatore ha risposte non lette ai suoi ticket
     * @param player Il giocatore da controllare
     */
    private void checkUnreadReplies(Player player) {
        Map<Integer, Integer> unreadTickets = plugin.getDatabaseManager().getUnreadRepliesForPlayer(player.getUniqueId());
        
        if (!unreadTickets.isEmpty()) {
            // Costruisci il messaggio di notifica
            String prefix = plugin.getMessageManager().getMessage("prefix");
            player.sendMessage(plugin.getMessageManager().getMessage("login-notification-unread-replies-header"));
            
            for (Map.Entry<Integer, Integer> entry : unreadTickets.entrySet()) {
                int ticketId = entry.getKey();
                int unreadCount = entry.getValue();
                
                // Messaggio cliccabile per visualizzare il ticket
                TextComponent message = new TextComponent(plugin.getMessageManager().formatMessage("login-notification-unread-replies-item", 
                        Map.of("id", String.valueOf(ticketId), "count", String.valueOf(unreadCount))));
                        
                message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                        new ComponentBuilder("§7Clicca per visualizzare il ticket").create()));
                message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, 
                        "/ticket info " + ticketId));
                
                player.spigot().sendMessage(message);
            }
        }
    }
    
    /**
     * Controlla i ticket che necessitano di risposta e notifica i membri dello staff
     * @param player Il membro dello staff da notificare
     */
    private void checkPendingTickets(Player player) {
        List<Ticket> pendingTickets = plugin.getDatabaseManager().getPendingTickets();
        
        if (!pendingTickets.isEmpty()) {
            // Costruisci il messaggio di notifica
            String prefix = plugin.getMessageManager().getMessage("prefix");
            String message;
            
            if (pendingTickets.size() == 1) {
                message = plugin.getMessageManager().formatMessage("login-notification-staff-single",
                        Map.of("id", String.valueOf(pendingTickets.get(0).getId())));
            } else {
                message = plugin.getMessageManager().formatMessage("login-notification-staff-multiple",
                        Map.of("count", String.valueOf(pendingTickets.size())));
            }
            
            // Crea il messaggio cliccabile
            TextComponent component = new TextComponent(message);
            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                    new ComponentBuilder("§7Clicca per visualizzare i ticket").create()));
            component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, 
                    "/ticket list"));
                    
            player.spigot().sendMessage(component);
        }
    }
    
    /**
     * Controlla se il giocatore ha ticket chiusi che necessitano di feedback
     * @param player Il giocatore da controllare
     */
    private void checkPendingFeedback(Player player) {
        List<Ticket> pendingFeedback = plugin.getDatabaseManager().getPendingFeedbackTickets(player.getUniqueId());
        
        if (!pendingFeedback.isEmpty()) {
            // Costruisci il messaggio di notifica
            player.sendMessage(plugin.getMessageManager().getMessage("login-notification-pending-feedback-header"));
            
            for (int i = 0; i < Math.min(pendingFeedback.size(), 3); i++) { // Mostra massimo 3 ticket
                Ticket ticket = pendingFeedback.get(i);
                
                // Messaggio cliccabile per dare feedback
                TextComponent message = new TextComponent(plugin.getMessageManager().formatMessage("login-notification-pending-feedback-item", 
                        Map.of("id", String.valueOf(ticket.getId()))));
                        
                message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                        new ComponentBuilder("§7Clicca per dare un feedback").create()));
                message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, 
                        "/ticket feedback " + ticket.getId()));
                
                player.spigot().sendMessage(message);
            }
            
            if (pendingFeedback.size() > 3) {
                player.sendMessage(plugin.getMessageManager().formatMessage("login-notification-pending-feedback-more", 
                        Map.of("count", String.valueOf(pendingFeedback.size() - 3))));
            }
        }
    }
}
