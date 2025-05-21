package it.Gecky.gktickets.utils;

import it.Gecky.gktickets.GKTickets;
import it.Gecky.gktickets.models.Reply;
import it.Gecky.gktickets.models.Ticket;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class MessageManager {

    private final GKTickets plugin;
    private File messagesFile;
    private FileConfiguration messagesConfig;

    public MessageManager(GKTickets plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    public void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
        
        // Controlla che ci siano tutti i messaggi di default
        InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream));
            
            for (String key : defaultConfig.getKeys(true)) {
                if (!messagesConfig.contains(key)) {
                    messagesConfig.set(key, defaultConfig.get(key));
                }
            }
            
            try {
                messagesConfig.save(messagesFile);
            } catch (IOException e) {
                plugin.getLogger().severe("Impossibile salvare messages.yml: " + e.getMessage());
            }
        }
    }
    
    public void reloadMessages() {
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }
    
    public String getMessage(String path) {
        String message = messagesConfig.getString(path, "Messaggio non trovato: " + path);
        // Sostituisci il prefisso e il divisore prima di altri placeholder
        String prefix = messagesConfig.getString("prefix", "&8[&6&lGK&e&lTickets&8]");
        String divider = messagesConfig.getString("divider", "&8&m--------------------");
        message = message.replace("{prefix}", prefix).replace("{divider}", divider);
        return ChatColor.translateAlternateColorCodes('&', message);
    }
    
    public String formatMessage(String path, Map<String, String> placeholders) {
        String message = getMessage(path);
        
        for (Map.Entry<String, String> placeholder : placeholders.entrySet()) {
            message = message.replace("{" + placeholder.getKey() + "}", placeholder.getValue());
        }
        
        return message;
    }
    
    public String formatWithPlaceholders(Player player, String message) {
        if (plugin.isPlaceholderAPIEnabled() && player != null) {
            return PlaceholderAPI.setPlaceholders(player, message);
        }
        return message;
    }
    
    public String getTicketMessage(String path, Ticket ticket) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("id", String.valueOf(ticket.getId()));
        placeholders.put("player", ticket.getPlayerName());
        placeholders.put("description", ticket.getDescription());
        placeholders.put("status", ticket.getStatus());
        placeholders.put("created_at", ticket.getCreatedAt());
        
        return formatMessage(path, placeholders);
    }
    
    public String getReplyMessage(String path, Reply reply) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("id", String.valueOf(reply.getId()));
        placeholders.put("ticket_id", String.valueOf(reply.getTicketId()));
        placeholders.put("player", reply.getPlayerName());
        placeholders.put("message", reply.getMessage());
        placeholders.put("created_at", reply.getCreatedAt());
        
        return formatMessage(path, placeholders);
    }
}
