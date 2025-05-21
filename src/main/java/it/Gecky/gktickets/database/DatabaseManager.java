package it.Gecky.gktickets.database;

import it.Gecky.gktickets.GKTickets;
import it.Gecky.gktickets.models.Reply;
import it.Gecky.gktickets.models.Ticket;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DatabaseManager {
    
    private final GKTickets plugin;
    private Connection connection;
    
    public DatabaseManager(GKTickets plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Inizializza il database e crea le tabelle necessarie
     */
    public void initialize() {
        try {
            // Crea la cartella database se non esiste
            File dataFolder = new File(plugin.getDataFolder(), "database");
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            
            // Crea la connessione al database
            String url = "jdbc:sqlite:" + dataFolder.getAbsolutePath() + File.separator + "tickets.db";
            connection = DriverManager.getConnection(url);
            
            if (connection != null) {
                plugin.getLogger().info("Connessione al database stabilita.");
                createTables();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore durante la connessione al database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Crea le tabelle necessarie nel database
     */
    private void createTables() {
        try (Statement statement = connection.createStatement()) {
            // Tabella dei ticket con aggiunta del campo categoria
            statement.execute(
                "CREATE TABLE IF NOT EXISTS tickets (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "player_uuid VARCHAR(36) NOT NULL, " +
                "player_name VARCHAR(16) NOT NULL, " +
                "category VARCHAR(32) DEFAULT 'general', " +  // Campo per la categoria
                "description TEXT NOT NULL, " +
                "status VARCHAR(10) DEFAULT 'OPEN', " +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)"
            );
            
            // Controlla se la colonna categoria esiste già
            try {
                statement.execute("SELECT category FROM tickets LIMIT 1");
            } catch (SQLException e) {
                // La colonna non esiste, quindi la aggiungiamo
                statement.execute("ALTER TABLE tickets ADD COLUMN category VARCHAR(32) DEFAULT 'general'");
                plugin.getLogger().info("Aggiunta colonna categoria alla tabella tickets");
            }
            
            // Tabella delle risposte
            statement.execute(
                "CREATE TABLE IF NOT EXISTS replies (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "ticket_id INTEGER NOT NULL, " +
                "player_uuid VARCHAR(36) NOT NULL, " +
                "player_name VARCHAR(16) NOT NULL, " +
                "message TEXT NOT NULL, " +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (ticket_id) REFERENCES tickets(id))"
            );
            
            // Tabella dei feedback sui ticket
            statement.execute(
                "CREATE TABLE IF NOT EXISTS ticket_feedback (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "ticket_id INTEGER NOT NULL, " +
                "player_uuid VARCHAR(36) NOT NULL, " +
                "rating INTEGER NOT NULL, " +
                "comment TEXT, " +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (ticket_id) REFERENCES tickets(id))"
            );
            
            // Tabella per tenere traccia delle visualizzazioni dei ticket
            statement.execute(
                "CREATE TABLE IF NOT EXISTS ticket_views (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "ticket_id INTEGER NOT NULL, " +
                "player_uuid VARCHAR(36) NOT NULL, " +
                "last_viewed_reply_id INTEGER, " +
                "last_viewed_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (ticket_id) REFERENCES tickets(id))"
            );
            
            plugin.getLogger().info("Tabelle del database create o verificate con successo.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore durante la creazione delle tabelle: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Chiude la connessione al database
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Connessione al database chiusa.");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore durante la chiusura della connessione al database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Crea un nuovo ticket
     * @param player Giocatore che crea il ticket
     * @param description Descrizione del ticket
     * @return ID del ticket creato o -1 se c'è stato un errore
     */
    public int createTicket(Player player, String description) {
        return createTicket(player, "general", description);
    }
    
    /**
     * Crea un nuovo ticket con categoria
     * @param player Giocatore che crea il ticket
     * @param category Categoria del ticket
     * @param description Descrizione del ticket
     * @return ID del ticket creato o -1 se c'è stato un errore
     */
    public int createTicket(Player player, String category, String description) {
        int ticketId = -1;
        String sql = "INSERT INTO tickets (player_uuid, player_name, category, description, status, created_at) VALUES (?, ?, ?, ?, 'OPEN', ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, player.getUniqueId().toString());
            pstmt.setString(2, player.getName());
            pstmt.setString(3, category);
            pstmt.setString(4, description);
            pstmt.setString(5, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            pstmt.executeUpdate();
            
            ResultSet rs = pstmt.getGeneratedKeys();
            if (rs.next()) {
                ticketId = rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore durante la creazione del ticket: " + e.getMessage());
            e.printStackTrace();
        }
        
        return ticketId;
    }
    
    /**
     * Adds a reply from a player to a ticket
     * @param ticketId ID of the ticket
     * @param playerUuid UUID of the player
     * @param message Reply message
     * @return true if successful
     */
    public boolean addReplyToTicket(int ticketId, UUID playerUuid, String message) {
        // Get player name from UUID
        String playerName = "Unknown";
        try {
            Player player = Bukkit.getPlayer(playerUuid);
            if (player != null) {
                playerName = player.getName();
            } else {
                // Try to get from offline player data
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUuid);
                if (offlinePlayer.hasPlayedBefore()) {
                    playerName = offlinePlayer.getName();
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Could not get player name for UUID: " + playerUuid);
        }
        
        // Insert reply into database
        String sql = "INSERT INTO replies (ticket_id, player_uuid, player_name, message, created_at) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, ticketId);
            pstmt.setString(2, playerUuid.toString());
            pstmt.setString(3, playerName);
            pstmt.setString(4, message);
            pstmt.setString(5, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("Error adding reply to ticket: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Conta i ticket aperti per un determinato utente
     * @param playerUuid UUID del giocatore
     * @return Numero di ticket aperti
     */
    public int countOpenTicketsForUser(UUID playerUuid) {
        String sql = "SELECT COUNT(*) FROM tickets WHERE player_uuid = ? AND status = 'OPEN'";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid.toString());
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore durante il conteggio dei ticket aperti: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0;
    }
    
    /**
     * Ottiene un ticket dal suo ID
     * @param ticketId ID del ticket
     * @return Il ticket o null se non trovato
     */
    public Ticket getTicketById(int ticketId) {
        Ticket ticket = null;
        String sql = "SELECT * FROM tickets WHERE id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, ticketId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String category = "general";
                try {
                    category = rs.getString("category");
                    if (category == null) category = "general";
                } catch (SQLException ex) {
                    // La colonna categoria potrebbe non esistere nelle versioni precedenti
                    category = "general";
                }
                
                ticket = new Ticket(
                    rs.getInt("id"),
                    UUID.fromString(rs.getString("player_uuid")),
                    rs.getString("player_name"),
                    category,
                    rs.getString("description"),
                    rs.getString("status"),
                    rs.getString("created_at")
                );
                
                // Carica le risposte per questo ticket
                ticket.setReplies(getRepliesForTicket(ticketId));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore durante il recupero del ticket: " + e.getMessage());
            e.printStackTrace();
        }
        
        return ticket;
    }
    
    /**
     * Ottiene tutte le risposte per un ticket
     * @param ticketId ID del ticket
     * @return Lista delle risposte
     */
    public List<Reply> getRepliesForTicket(int ticketId) {
        List<Reply> replies = new ArrayList<>();
        String sql = "SELECT * FROM replies WHERE ticket_id = ? ORDER BY created_at ASC";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, ticketId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Reply reply = new Reply(
                    rs.getInt("id"),
                    rs.getInt("ticket_id"),
                    UUID.fromString(rs.getString("player_uuid")),
                    rs.getString("player_name"),
                    rs.getString("message"),
                    rs.getString("created_at")
                );
                replies.add(reply);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore durante il recupero delle risposte: " + e.getMessage());
            e.printStackTrace();
        }
        
        return replies;
    }
    
    /**
     * Ottiene tutti i ticket aperti
     * @return Lista dei ticket aperti
     */
    public List<Ticket> getOpenTickets() {
        List<Ticket> tickets = new ArrayList<>();
        String sql = "SELECT * FROM tickets WHERE status = 'OPEN' ORDER BY created_at DESC";
        
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                String category = "general";
                try {
                    category = rs.getString("category");
                    if (category == null) category = "general";
                } catch (SQLException ex) {
                    // La colonna categoria potrebbe non esistere nelle versioni precedenti
                    category = "general";
                }
                
                Ticket ticket = new Ticket(
                    rs.getInt("id"),
                    UUID.fromString(rs.getString("player_uuid")),
                    rs.getString("player_name"),
                    category,
                    rs.getString("description"),
                    rs.getString("status"),
                    rs.getString("created_at")
                );
                tickets.add(ticket);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore durante il recupero dei ticket aperti: " + e.getMessage());
            e.printStackTrace();
        }
        
        return tickets;
    }
    
    /**
     * Ottiene tutti i ticket di un giocatore
     * @param playerUuid UUID del giocatore
     * @return Lista dei ticket del giocatore
     */
    public List<Ticket> getPlayerTickets(UUID playerUuid) {
        List<Ticket> tickets = new ArrayList<>();
        String sql = "SELECT * FROM tickets WHERE player_uuid = ? ORDER BY created_at DESC";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid.toString());
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                String category = "general";
                try {
                    category = rs.getString("category");
                    if (category == null) category = "general";
                } catch (SQLException ex) {
                    // La colonna categoria potrebbe non esistere nelle versioni precedenti
                    category = "general";
                }
                
                Ticket ticket = new Ticket(
                    rs.getInt("id"),
                    UUID.fromString(rs.getString("player_uuid")),
                    rs.getString("player_name"),
                    category,
                    rs.getString("description"),
                    rs.getString("status"),
                    rs.getString("created_at")
                );
                tickets.add(ticket);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore durante il recupero dei ticket del giocatore: " + e.getMessage());
            e.printStackTrace();
        }
        
        return tickets;
    }
    
    /**
     * Chiude un ticket
     * @param ticketId ID del ticket
     * @return true se la chiusura è avvenuta con successo
     */
    public boolean closeTicket(int ticketId) {
        String sql = "UPDATE tickets SET status = 'CLOSED' WHERE id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, ticketId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore durante la chiusura del ticket: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Aggiunge una risposta a un ticket
     * @param ticketId ID del ticket
     * @param player Giocatore che risponde
     * @param message Messaggio della risposta
     * @return true se la risposta è stata aggiunta con successo
     */
    public boolean addReply(int ticketId, Player player, String message) {
        String sql = "INSERT INTO replies (ticket_id, player_uuid, player_name, message, created_at) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, ticketId);
            pstmt.setString(2, player.getUniqueId().toString());
            pstmt.setString(3, player.getName());
            pstmt.setString(4, message);
            pstmt.setString(5, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore durante l'aggiunta della risposta: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Ottiene i ticket di un giocatore per nome
     * @param playerName Nome del giocatore
     * @return Lista dei ticket del giocatore
     */
    public List<Ticket> getPlayerTicketsByName(String playerName) {
        List<Ticket> tickets = new ArrayList<>();
        String sql = "SELECT * FROM tickets WHERE player_name = ? COLLATE NOCASE ORDER BY created_at DESC";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                String category = "general";
                try {
                    category = rs.getString("category");
                    if (category == null) category = "general";
                } catch (SQLException ex) {
                    // La colonna categoria potrebbe non esistere nelle versioni precedenti
                    category = "general";
                }
                
                Ticket ticket = new Ticket(
                    rs.getInt("id"),
                    UUID.fromString(rs.getString("player_uuid")),
                    rs.getString("player_name"),
                    category,
                    rs.getString("description"),
                    rs.getString("status"),
                    rs.getString("created_at")
                );
                tickets.add(ticket);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore durante il recupero dei ticket per nome: " + e.getMessage());
            e.printStackTrace();
        }
        
        return tickets;
    }
    
    /**
     * Salva il feedback per un ticket
     * @param ticketId ID del ticket
     * @param playerUuid UUID del giocatore
     * @param rating Valutazione da 1 a 5
     * @param comment Commento opzionale
     * @return true se il salvataggio è avvenuto con successo
     */
    public boolean saveTicketFeedback(int ticketId, UUID playerUuid, int rating, String comment) {
        String sql = "INSERT INTO ticket_feedback (ticket_id, player_uuid, rating, comment, created_at) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, ticketId);
            pstmt.setString(2, playerUuid.toString());
            pstmt.setInt(3, rating);
            pstmt.setString(4, comment);
            pstmt.setString(5, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore durante il salvataggio del feedback: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Verifica se un ticket ha già ricevuto feedback
     * @param ticketId ID del ticket
     * @return true se il ticket ha già ricevuto feedback
     */
    public boolean hasTicketFeedback(int ticketId) {
        String sql = "SELECT id FROM ticket_feedback WHERE ticket_id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, ticketId);
            ResultSet rs = pstmt.executeQuery();
            
            return rs.next();
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore durante la verifica del feedback: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Ottiene i ticket chiusi che richiedono ancora feedback dal giocatore
     * @param playerUuid UUID del giocatore
     * @return Lista di ticket che richiedono ancora feedback
     */
    public List<Ticket> getPendingFeedbackTickets(UUID playerUuid) {
        List<Ticket> pendingFeedback = new ArrayList<>();
        String sql = "SELECT t.* FROM tickets t " +
                     "LEFT JOIN ticket_feedback f ON t.id = f.ticket_id " +
                     "WHERE t.player_uuid = ? " +
                     "AND t.status = 'CLOSED' " +
                     "AND f.id IS NULL " +
                     "ORDER BY t.created_at DESC";
                     
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid.toString());
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                String category = "general";
                try {
                    category = rs.getString("category");
                    if (category == null) category = "general";
                } catch (SQLException ex) {
                    // La colonna categoria potrebbe non esistere nelle versioni precedenti
                    category = "general";
                }
                
                Ticket ticket = new Ticket(
                    rs.getInt("id"),
                    UUID.fromString(rs.getString("player_uuid")),
                    rs.getString("player_name"),
                    category,
                    rs.getString("description"),
                    rs.getString("status"),
                    rs.getString("created_at")
                );
                pendingFeedback.add(ticket);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore nel recupero dei ticket in attesa di feedback: " + e.getMessage());
            e.printStackTrace();
        }
        
        return pendingFeedback;
    }
    
    /**
     * Ottiene una mappa di ticket che hanno risposte non lette per un giocatore
     * @param playerUuid UUID del giocatore
     * @return Map<ticketId, conteggio risposte non lette>
     */
    public Map<Integer, Integer> getUnreadRepliesForPlayer(UUID playerUuid) {
        Map<Integer, Integer> unreadReplies = new HashMap<>();
        
        String sql = "SELECT t.id, " +
                    "COUNT(r.id) as unread_count " +
                    "FROM tickets t " +
                    "JOIN replies r ON t.id = r.ticket_id " +
                    "LEFT JOIN ticket_views tv ON t.id = tv.ticket_id AND tv.player_uuid = ? " +
                    "WHERE t.player_uuid = ? " +
                    "AND t.status = 'OPEN' " +
                    "AND r.player_uuid != ? " +
                    "AND (tv.last_viewed_reply_id IS NULL OR r.id > tv.last_viewed_reply_id) " +
                    "GROUP BY t.id";
                    
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUuid.toString());
            pstmt.setString(2, playerUuid.toString());
            pstmt.setString(3, playerUuid.toString());
            
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                unreadReplies.put(rs.getInt("id"), rs.getInt("unread_count"));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore durante il recupero delle risposte non lette: " + e.getMessage());
            e.printStackTrace();
        }
        
        return unreadReplies;
    }
    
    /**
     * Ottiene la lista dei ticket aperti che richiedono attenzione dello staff
     * @return Lista di ticket in attesa di risposta
     */
    public List<Ticket> getPendingTickets() {
        List<Ticket> pendingTickets = new ArrayList<>();
        
        String sql = "SELECT t.* FROM tickets t " +
                    "LEFT JOIN ( " +
                    "   SELECT r.ticket_id, MAX(r.id) as last_reply_id, r.player_uuid " +
                    "   FROM replies r " +
                    "   GROUP BY r.ticket_id " +
                    ") lr ON t.id = lr.ticket_id " +
                    "WHERE t.status = 'OPEN' AND " +
                    "      (lr.last_reply_id IS NULL OR " +
                    "       lr.player_uuid = t.player_uuid)";
                    
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                String category = "general";
                try {
                    category = rs.getString("category");
                    if (category == null) category = "general";
                } catch (SQLException ex) {
                    // La colonna categoria potrebbe non esistere nelle versioni precedenti
                    category = "general";
                }
                
                Ticket ticket = new Ticket(
                    rs.getInt("id"),
                    UUID.fromString(rs.getString("player_uuid")),
                    rs.getString("player_name"),
                    category,
                    rs.getString("description"),
                    rs.getString("status"),
                    rs.getString("created_at")
                );
                pendingTickets.add(ticket);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore durante il recupero dei ticket in attesa: " + e.getMessage());
            e.printStackTrace();
        }
        
        return pendingTickets;
    }
    
    /**
     * Segna un ticket come visualizzato da un giocatore
     * @param ticketId ID del ticket
     * @param player Giocatore che visualizza
     */
    public void markTicketAsViewed(int ticketId, Player player) {
        // Get the last reply ID for this ticket
        int lastReplyId = 0;
        List<Reply> replies = getRepliesForTicket(ticketId);
        if (!replies.isEmpty()) {
            lastReplyId = replies.get(replies.size() - 1).getId();
        }
        
        String sql = "INSERT OR REPLACE INTO ticket_views (ticket_id, player_uuid, last_viewed_reply_id, last_viewed_at) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, ticketId);
            pstmt.setString(2, player.getUniqueId().toString());
            pstmt.setInt(3, lastReplyId);
            pstmt.setString(4, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore durante il salvataggio della visualizzazione: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Ottiene le statistiche dei ticket
     * @return Map con le statistiche
     */
    public Map<String, Object> getTicketStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Conteggio totale ticket
            try (Statement stmt = connection.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM tickets");
                if (rs.next()) {
                    stats.put("total", rs.getInt(1));
                }
            }
            
            // Conteggio ticket aperti
            try (Statement stmt = connection.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM tickets WHERE status = 'OPEN'");
                if (rs.next()) {
                    stats.put("open", rs.getInt(1));
                }
            }
            
            // Conteggio ticket chiusi
            try (Statement stmt = connection.createStatement()) {
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM tickets WHERE status = 'CLOSED'");
                if (rs.next()) {
                    stats.put("closed", rs.getInt(1));
                }
            }
            
            // Valutazione media
            try (Statement stmt = connection.createStatement()) {
                ResultSet rs = stmt.executeQuery(
                    "SELECT AVG(rating) FROM ticket_feedback"
                );
                if (rs.next()) {
                    double avgRating = rs.getDouble(1);
                    if (!rs.wasNull()) {
                        stats.put("avg_rating", String.format("%.1f", avgRating));
                    } else {
                        stats.put("avg_rating", "N/A");
                    }
                } else {
                    stats.put("avg_rating", "N/A");
                }
            }
            
            // Distribuzione delle valutazioni
            Map<Integer, Integer> ratingDistribution = new HashMap<>();
            for (int i = 1; i <= 5; i++) {
                ratingDistribution.put(i, 0);
            }
            
            try (Statement stmt = connection.createStatement()) {
                ResultSet rs = stmt.executeQuery(
                    "SELECT rating, COUNT(*) FROM ticket_feedback GROUP BY rating"
                );
                while (rs.next()) {
                    int rating = rs.getInt(1);
                    int count = rs.getInt(2);
                    ratingDistribution.put(rating, count);
                }
            }
            stats.put("rating_distribution", ratingDistribution);
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore nel recupero delle statistiche: " + e.getMessage());
            e.printStackTrace();
        }
        
        return stats;
    }
    
    /**
     * Formatta il tempo in minuti in una stringa leggibile
     * @param minutes Minuti
     * @return Tempo formattato
     */
    private String formatTime(double minutes) {
        if (minutes < 60) {
            return String.format("%.0f min", minutes);
        } else if (minutes < 1440) {
            double hours = minutes / 60;
            return String.format("%.1f ore", hours);
        } else {
            double days = minutes / 1440;
            return String.format("%.1f giorni", days);
        }
    }
    
    /**
     * Get the last activity time for a ticket (creation date or latest reply)
     * @param ticketId The ticket ID
     * @return Timestamp string of last activity
     */
    public String getLastActivityTime(int ticketId) {
        String sql = "SELECT COALESCE(" +
                    "(SELECT MAX(created_at) FROM replies WHERE ticket_id = ?), " +
                    "(SELECT created_at FROM tickets WHERE id = ?)) AS last_activity";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, ticketId);
            pstmt.setInt(2, ticketId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return rs.getString("last_activity");
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error getting last activity time: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Close a ticket with specified closer and reason
     * @param ticketId The ticket ID
     * @param closedBy Who closed the ticket
     * @param reason Reason for closing
     * @return true if successful
     */
    public boolean closeTicket(int ticketId, String closedBy, String reason) {
        String sql = "UPDATE tickets SET status = 'CLOSED', closed_by = ?, closed_reason = ?, closed_at = ? WHERE id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, closedBy);
            pstmt.setString(2, reason);
            pstmt.setString(3, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            pstmt.setInt(4, ticketId);
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("Error closing ticket: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Get the database connection
     * @return The database connection
     */
    public Connection getConnection() {
        return connection;
    }
}