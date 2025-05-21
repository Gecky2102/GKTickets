package it.Gecky.gktickets.notifications;

import it.Gecky.gktickets.GKTickets;
import it.Gecky.gktickets.models.Ticket;
import it.Gecky.gktickets.utils.MessageManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

public class NotificationManager {

    private final GKTickets plugin;
    private final MessageManager messageManager;

    public NotificationManager(GKTickets plugin) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
    }
    
    public void notifyStaffNewTicket(Ticket ticket) {
        if (!plugin.getConfigManager().isNotifyStaffOnNewTicket()) return;
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("id", String.valueOf(ticket.getId()));
        placeholders.put("player", ticket.getPlayerName());
        
        String message = messageManager.formatMessage("notification-new-ticket", placeholders);
        
        TextComponent component = new TextComponent(message);
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§7Clicca per visualizzare i dettagli").create()));
        component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ticket info " + ticket.getId()));
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("gktickets.staff")) {
                player.spigot().sendMessage(component);
                
                if (plugin.getConfigManager().isSoundEnabled()) {
                    try {
                        player.playSound(player.getLocation(), Sound.valueOf(plugin.getConfigManager().getNewTicketSound().toUpperCase()), 1.0f, 1.0f);
                    } catch (Exception ignored) {
                        // Il suono potrebbe non esistere
                    }
                }
            }
        }
    }
    
    public void notifyStaffNewReply(Ticket ticket, String responderName) {
        if (!plugin.getConfigManager().isNotifyStaffOnReply()) return;
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("id", String.valueOf(ticket.getId()));
        placeholders.put("player", responderName);
        
        String message = messageManager.formatMessage("notification-new-reply-staff", placeholders);
        
        TextComponent component = new TextComponent(message);
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§7Clicca per visualizzare la risposta").create()));
        component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ticket info " + ticket.getId()));
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("gktickets.staff") && !player.getName().equals(responderName)) {
                player.spigot().sendMessage(component);
                
                if (plugin.getConfigManager().isSoundEnabled()) {
                    try {
                        player.playSound(player.getLocation(), Sound.valueOf(plugin.getConfigManager().getNewReplySound().toUpperCase()), 1.0f, 1.0f);
                    } catch (Exception ignored) {
                        // Il suono potrebbe non esistere
                    }
                }
            }
        }
    }
    
    public void notifyPlayerTicketClosed(Ticket ticket, Player closer) {
        if (!plugin.getConfigManager().isNotifyPlayerOnClose()) return;
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("id", String.valueOf(ticket.getId()));
        placeholders.put("player", closer.getName());
        
        String message = messageManager.formatMessage("notification-ticket-closed", placeholders);
        
        Player ticketCreator = Bukkit.getPlayer(ticket.getPlayerUuid());
        if (ticketCreator != null && ticketCreator.isOnline() && !ticketCreator.equals(closer)) {
            message = messageManager.formatWithPlaceholders(ticketCreator, message);
            
            // Rendi la notifica cliccabile per visualizzare il ticket
            TextComponent component = new TextComponent(message);
            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§7Clicca per visualizzare il ticket").create()));
            component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ticket info " + ticket.getId()));
            
            ticketCreator.spigot().sendMessage(component);
        }
    }
    
    public void notifyPlayerNewReply(Ticket ticket, Player player) {
        if (!plugin.getConfigManager().isNotifyPlayerOnReply()) return;
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("id", String.valueOf(ticket.getId()));
        placeholders.put("player", player.getName());
        
        String message = messageManager.formatMessage("notification-new-reply-player", placeholders);
        
        Player creator = Bukkit.getPlayer(ticket.getPlayerUuid());
        if (creator != null && creator.isOnline() && !creator.equals(player)) {
            message = messageManager.formatWithPlaceholders(creator, message);
            
            TextComponent component = new TextComponent(message);
            component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("§7Clicca per visualizzare la risposta").create()));
            component.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/ticket info " + ticket.getId()));
            
            creator.spigot().sendMessage(component);
            
            if (plugin.getConfigManager().isSoundEnabled()) {
                try {
                    creator.playSound(creator.getLocation(), Sound.valueOf(plugin.getConfigManager().getNewReplySound().toUpperCase()), 1.0f, 1.0f);
                } catch (Exception ignored) {
                    // Il suono potrebbe non esistere
                }
            }
        }
    }
}
