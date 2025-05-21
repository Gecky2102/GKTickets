package it.Gecky.gktickets.integrations;

import it.Gecky.gktickets.GKTickets;
import it.Gecky.gktickets.models.Reply;
import it.Gecky.gktickets.models.Ticket;
import org.bukkit.entity.Player;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class DiscordIntegration {
    
    private final GKTickets plugin;
    private final String webhookUrl;
    private final String username;
    private final String avatarUrl;
    private final boolean enabled;
    private final boolean logTicketCreate;
    private final boolean logTicketClose;
    private final boolean logTicketReply;
    private final String colorCreate;
    private final String colorReply;
    private final String colorClose;
    
    public DiscordIntegration(GKTickets plugin) {
        this.plugin = plugin;
        
        this.enabled = plugin.getConfig().getBoolean("discord.enabled", false);
        this.webhookUrl = plugin.getConfig().getString("discord.webhook-url", "");
        this.username = plugin.getConfig().getString("discord.username", "GKTickets");
        this.avatarUrl = plugin.getConfig().getString("discord.avatar-url", "");
        
        this.logTicketCreate = plugin.getConfig().getBoolean("discord.log-events.ticket-create", true);
        this.logTicketClose = plugin.getConfig().getBoolean("discord.log-events.ticket-close", true);
        this.logTicketReply = plugin.getConfig().getBoolean("discord.log-events.ticket-reply", true);
        
        this.colorCreate = plugin.getConfig().getString("discord.colors.create", "#00FF00");
        this.colorReply = plugin.getConfig().getString("discord.colors.reply", "#0099FF");
        this.colorClose = plugin.getConfig().getString("discord.colors.close", "#FF0000");
    }
    
    /**
     * Controlla se l'integrazione Discord è abilitata
     * @return true se abilitata, false altrimenti
     */
    public boolean isEnabled() {
        return enabled && !webhookUrl.isEmpty();
    }
    
    /**
     * Invia un messaggio quando viene creato un nuovo ticket
     * @param ticket Il ticket creato
     * @param player Il giocatore che ha creato il ticket
     */
    public void sendTicketCreatedMessage(Ticket ticket, Player player) {
        if (!isEnabled() || !logTicketCreate) return;
        
        JSONObject embed = new JSONObject();
        embed.put("title", "Nuovo Ticket #" + ticket.getId());
        embed.put("description", ticket.getDescription());
        embed.put("color", hexToDecimal(colorCreate));
        
        JSONArray fields = new JSONArray();
        
        // Campo per il creatore del ticket
        JSONObject creatorField = new JSONObject();
        creatorField.put("name", "Creato da");
        creatorField.put("value", player.getName());
        creatorField.put("inline", true);
        fields.add(creatorField);
        
        // Campo per la data di creazione
        JSONObject dateField = new JSONObject();
        dateField.put("name", "Data");
        dateField.put("value", ticket.getCreatedAt());
        dateField.put("inline", true);
        fields.add(dateField);
        
        embed.put("fields", fields);
        
        // Footer con informazioni sul server
        JSONObject footer = new JSONObject();
        footer.put("text", "GKTickets v1.0.2 • Server: " + plugin.getServer().getName());
        embed.put("footer", footer);
        
        sendDiscordWebhook("Nuovo ticket creato", embed);
    }
    
    /**
     * Invia un messaggio quando viene aggiunta una risposta a un ticket
     * @param ticket Il ticket a cui è stata aggiunta la risposta
     * @param reply La risposta aggiunta
     * @param player Il giocatore che ha risposto
     */
    public void sendTicketReplyMessage(Ticket ticket, Reply reply, Player player) {
        if (!isEnabled() || !logTicketReply) return;
        
        JSONObject embed = new JSONObject();
        embed.put("title", "Risposta al Ticket #" + ticket.getId());
        embed.put("description", reply.getMessage());
        embed.put("color", hexToDecimal(colorReply));
        
        JSONArray fields = new JSONArray();
        
        // Campo per chi ha risposto
        JSONObject replierField = new JSONObject();
        replierField.put("name", "Risposta da");
        replierField.put("value", player.getName());
        replierField.put("inline", true);
        fields.add(replierField);
        
        // Campo per il creatore del ticket
        JSONObject creatorField = new JSONObject();
        creatorField.put("name", "Ticket creato da");
        creatorField.put("value", ticket.getPlayerName());
        creatorField.put("inline", true);
        fields.add(creatorField);
        
        // Campo per la data
        JSONObject dateField = new JSONObject();
        dateField.put("name", "Data risposta");
        dateField.put("value", reply.getCreatedAt());
        dateField.put("inline", true);
        fields.add(dateField);
        
        embed.put("fields", fields);
        
        // Footer con informazioni sul server
        JSONObject footer = new JSONObject();
        footer.put("text", "GKTickets v1.0.2 • Server: " + plugin.getServer().getName());
        embed.put("footer", footer);
        
        sendDiscordWebhook("Nuova risposta a un ticket", embed);
    }
    
    /**
     * Invia un messaggio quando viene chiuso un ticket
     * @param ticket Il ticket chiuso
     * @param player Il giocatore che ha chiuso il ticket
     */
    public void sendTicketClosedMessage(Ticket ticket, Player player) {
        if (!isEnabled() || !logTicketClose) return;
        
        JSONObject embed = new JSONObject();
        embed.put("title", "Ticket #" + ticket.getId() + " Chiuso");
        embed.put("description", "Il ticket è stato chiuso da " + player.getName());
        embed.put("color", hexToDecimal(colorClose));
        
        JSONArray fields = new JSONArray();
        
        // Campo per il creatore del ticket
        JSONObject creatorField = new JSONObject();
        creatorField.put("name", "Ticket creato da");
        creatorField.put("value", ticket.getPlayerName());
        creatorField.put("inline", true);
        fields.add(creatorField);
        
        // Campo per chi ha chiuso il ticket
        JSONObject closerField = new JSONObject();
        closerField.put("name", "Chiuso da");
        closerField.put("value", player.getName());
        closerField.put("inline", true);
        fields.add(closerField);
        
        // Campo per la descrizione del ticket
        JSONObject descriptionField = new JSONObject();
        descriptionField.put("name", "Descrizione ticket");
        descriptionField.put("value", ticket.getDescription().length() > 100 ? 
                            ticket.getDescription().substring(0, 100) + "..." : 
                            ticket.getDescription());
        descriptionField.put("inline", false);
        fields.add(descriptionField);
        
        embed.put("fields", fields);
        
        // Footer con informazioni sul server
        JSONObject footer = new JSONObject();
        footer.put("text", "GKTickets v1.0.2 • Server: " + plugin.getServer().getName());
        embed.put("footer", footer);
        
        sendDiscordWebhook("Ticket chiuso", embed);
    }
    
    /**
     * Invia un webhook a Discord
     * @param content Messaggio di testo principale
     * @param embed Oggetto embed da inviare
     */
    private void sendDiscordWebhook(String content, JSONObject embed) {
        // Esegui in un thread separato per non bloccare il server
        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL(webhookUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "GKTickets/1.0.2");
                connection.setDoOutput(true);
                
                JSONObject payload = new JSONObject();
                
                // Imposta username e avatar se specificati
                if (!username.isEmpty()) {
                    payload.put("username", username);
                }
                
                if (!avatarUrl.isEmpty()) {
                    payload.put("avatar_url", avatarUrl);
                }
                
                // Aggiungi il contenuto del messaggio
                payload.put("content", content);
                
                // Aggiungi l'embed
                JSONArray embeds = new JSONArray();
                embeds.add(embed);
                payload.put("embeds", embeds);
                
                // Invia la richiesta
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = payload.toJSONString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                
                // Leggi la risposta
                int responseCode = connection.getResponseCode();
                if (responseCode != 204) {
                    plugin.getLogger().warning("Errore nell'invio del webhook Discord: Codice " + responseCode);
                }
                
                connection.disconnect();
                
            } catch (IOException e) {
                plugin.getLogger().warning("Errore nell'invio del webhook Discord: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Converte un colore in formato esadecimale in decimale per Discord
     * @param hexColor Colore in formato #RRGGBB
     * @return Valore decimale del colore
     */
    private int hexToDecimal(String hexColor) {
        try {
            String cleanHex = hexColor.replace("#", "").trim();
            return Integer.parseInt(cleanHex, 16);
        } catch (NumberFormatException e) {
            // Ritorna rosso di default in caso di errore
            return 0xFF0000;
        }
    }
}
