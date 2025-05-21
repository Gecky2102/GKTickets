package it.Gecky.gktickets.database;

import it.Gecky.gktickets.GKTickets;
import it.Gecky.gktickets.models.Reply;
import it.Gecky.gktickets.models.Ticket;
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
    private final String databasePath;

    public DatabaseManager(GKTickets plugin) {
        this.plugin = plugin;
        this.databasePath = plugin.getDataFolder().getAbsolutePath() + File.separator + "tickets.db";
    }

    public void initialize() {
        try {
            // Carico il driver SQLite
            Class.forName("org.sqlite.JDBC");
            // Apro connessione al database
            connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
            
            // Creo le tabelle se non esistono
            createTables();
        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().severe("Errore durante l'inizializzazione del database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void createTables() {
        try (Statement statement = connection.createStatement()) {
            // Tabella dei ticket
            statement.execute(
                "CREATE TABLE IF NOT EXISTS tickets (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "player_uuid VARCHAR(36) NOT NULL, " +
                "player_name VARCHAR(16) NOT NULL, " +
                "description TEXT NOT NULL, " +
                "status VARCHAR(10) DEFAULT 'OPEN', " +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP)"
            );
            
            // Tabella delle risposte
            statement.execute(
                "CREATE TABLE IF NOT EXISTS replies (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "ticket_id INTEGER NOT NULL, " +
                "player_uuid VARCHAR(36) NOT NULL, " +
                "player_name VARCHAR(16) NOT NULL, " +
                "message TEXT NOT NULL, " +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY(ticket_id) REFERENCES tickets(id))"
            );
            
            // Tabella per tracciare le visualizzazioni dei ticket
            statement.execute(
                "CREATE TABLE IF NOT EXISTS ticket_views (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "ticket_id INTEGER NOT NULL, " +
                "player_uuid VARCHAR(36) NOT NULL, " +
                "last_viewed_reply_id INTEGER DEFAULT 0, " +
                "last_viewed_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY(ticket_id) REFERENCES tickets(id))"
            );
            
            // Tabella per i feedback
            statement.execute(
                "CREATE TABLE IF NOT EXISTS ticket_feedback (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "ticket_id INTEGER NOT NULL UNIQUE, " +
                "player_uuid VARCHAR(36) NOT NULL, " +
                "rating INTEGER, " + // Valutazione da 1 a 5
                "comment TEXT, " +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY(ticket_id) REFERENCES tickets(id))"
            );
            
            // Tabella per statistiche e timestamp
            statement.execute(
                "CREATE TABLE IF NOT EXISTS ticket_stats (" +
                "ticket_id INTEGER PRIMARY KEY, " +
                "first_reply_time DATETIME, " + // Timestamp prima risposta staff
                "resolution_time DATETIME, " +  // Timestamp risoluzione
                "first_response_seconds INTEGER, " + // Tempo di prima risposta in secondi
                "resolution_seconds INTEGER, " + // Tempo totale di risoluzione in secondi
                "FOREIGN KEY(ticket_id) REFERENCES tickets(id))"
            );
            
            plugin.getLogger().info("Tabelle del database create o verificate con successo.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore durante la creazione delle tabelle: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
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
    
    public int countOpenTicketsForUser(UUID playerUUID) {
        int count = 0;
        String sql = "SELECT COUNT(*) FROM tickets WHERE player_uuid = ? AND status = 'OPEN'";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                count = rs.getInt(1);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore durante il conteggio dei ticket aperti per l'utente: " + e.getMessage());
            e.printStackTrace();
        }
        
        return count;
    }
    
    public int createTicket(Player player, String description) {
        int ticketId = -1;
        String sql = "INSERT INTO tickets (player_uuid, player_name, description, status, created_at) VALUES (?, ?, ?, 'OPEN', ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, player.getUniqueId().toString());
            pstmt.setString(2, player.getName());
            pstmt.setString(3, description);
            pstmt.setString(4, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
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
    
    public Ticket getTicketById(int ticketId) {
        Ticket ticket = null;
        String sql = "SELECT * FROM tickets WHERE id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, ticketId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                ticket = new Ticket(
                    rs.getInt("id"),
                    UUID.fromString(rs.getString("player_uuid")),
                    rs.getString("player_name"),
                    rs.getString("description"),
                    rs.getString("status"),
                    rs.getString("created_at")
                );
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore durante il recupero del ticket: " + e.getMessage());
            e.printStackTrace();
        }
        
        return ticket;
    }
    
    public List<Ticket> getOpenTickets() {
        List<Ticket> tickets = new ArrayList<>();
        String sql = "SELECT * FROM tickets WHERE status = 'OPEN'";
        
        try (Statement stmt = connection.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                Ticket ticket = new Ticket(
                    rs.getInt("id"),
                    UUID.fromString(rs.getString("player_uuid")),
                    rs.getString("player_name"),
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
    
    public List<Ticket> getPlayerTickets(UUID playerUUID) {
        List<Ticket> tickets = new ArrayList<>();
        String sql = "SELECT * FROM tickets WHERE player_uuid = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Ticket ticket = new Ticket(
                    rs.getInt("id"),
                    UUID.fromString(rs.getString("player_uuid")),
                    rs.getString("player_name"),
                    rs.getString("description"),
                    rs.getString("status"),
                    rs.getString("created_at")
                );
                tickets.add(ticket);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore durante il recupero dei ticket dell'utente: " + e.getMessage());
            e.printStackTrace();
        }
        
        return tickets;
    }
    
    /**
     * Registra quando un ticket viene chiuso e calcola i tempi di risoluzione
     */
    public boolean closeTicket(int ticketId) {
        boolean success = false;
        String sql = "UPDATE tickets SET status = 'CLOSED' WHERE id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, ticketId);
            int rowsAffected = pstmt.executeUpdate();
            success = (rowsAffected > 0);
            
            if (success) {
                // Registra il timestamp di chiusura e calcola il tempo di risoluzione
                Ticket ticket = getTicketById(ticketId);
                if (ticket != null) {
                    updateTicketStats(ticket);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore durante la chiusura del ticket: " + e.getMessage());
            e.printStackTrace();
        }
        
        return success;
    }
    
    public boolean addReply(int ticketId, Player player, String message) {
        boolean success = false;
        String sql = "INSERT INTO replies (ticket_id, player_uuid, player_name, message, created_at) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, ticketId);
            pstmt.setString(2, player.getUniqueId().toString());
            pstmt.setString(3, player.getName());
            pstmt.setString(4, message);
            pstmt.setString(5, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            pstmt.executeUpdate();
            success = true;
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore durante l'aggiunta della risposta: " + e.getMessage());
            e.printStackTrace();
        }
        
        if (success) {
            // Se è una risposta dello staff, registra i tempi di risposta
            if (player.hasPermission("gktickets.staff")) {
                recordStaffReply(ticketId);
            }
        }
        
        return success;
    }
    
    /**
     * Registra i tempi della prima risposta staff
     */
    private void recordStaffReply(int ticketId) {
        Ticket ticket = getTicketById(ticketId);
        if (ticket == null) return;
        
        // Controlla se è la prima risposta dello staff
        String checkSql = "SELECT COUNT(*) FROM replies r " +
                          "JOIN tickets t ON t.id = r.ticket_id " +
                          "WHERE r.ticket_id = ? AND r.player_uuid != t.player_uuid";
        
        try (PreparedStatement pstmt = connection.prepareStatement(checkSql)) {
            pstmt.setInt(1, ticketId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next() && rs.getInt(1) == 1) { // È la prima risposta staff
                // Calcola il tempo trascorso dalla creazione del ticket
                long firstResponseTime = calculateResponseTime(ticket);
                updateFirstResponseTime(ticketId, firstResponseTime);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore durante il controllo delle risposte staff: " + e.getMessage());
        }
    }
    
    /**
     * Calcola il tempo di risposta in secondi
     */
    private long calculateResponseTime(Ticket ticket) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime createdAt = LocalDateTime.parse(ticket.getCreatedAt(), formatter);
            LocalDateTime now = LocalDateTime.now();
            return java.time.Duration.between(createdAt, now).getSeconds();
        } catch (Exception e) {
            plugin.getLogger().severe("Errore nel calcolo del tempo di risposta: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Aggiorna il tempo di prima risposta dello staff
     */
    private void updateFirstResponseTime(int ticketId, long responseTimeSeconds) {
        String sql = "INSERT OR REPLACE INTO ticket_stats " +
                     "(ticket_id, first_reply_time, first_response_seconds) " +
                     "VALUES (?, datetime('now'), ?)";
                     
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, ticketId);
            pstmt.setLong(2, responseTimeSeconds);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore nell'aggiornamento del tempo di risposta: " + e.getMessage());
        }
    }
    
    /**
     * Aggiorna le statistiche di un ticket (tempo di risoluzione, ecc.)
     */
    private void updateTicketStats(Ticket ticket) {
        String sql = "INSERT OR REPLACE INTO ticket_stats " +
                     "(ticket_id, resolution_time, resolution_seconds) " +
                     "VALUES (?, datetime('now'), ?)";
                     
        // Calcola il tempo di risoluzione
        long resolutionTime = calculateResolutionTime(ticket);
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, ticket.getId());
            pstmt.setLong(2, resolutionTime);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore nell'aggiornamento delle statistiche del ticket: " + e.getMessage());
        }
    }
    
    /**
     * Calcola il tempo di risoluzione in secondi
     */
    private long calculateResolutionTime(Ticket ticket) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime createdAt = LocalDateTime.parse(ticket.getCreatedAt(), formatter);
            LocalDateTime now = LocalDateTime.now();
            return java.time.Duration.between(createdAt, now).getSeconds();
        } catch (Exception e) {
            plugin.getLogger().severe("Errore nel calcolo del tempo di risoluzione: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Ottiene una mappa dei ticket con risposte non lette per un giocatore
     * @param playerUUID UUID del giocatore
     * @return Una mappa con chiave=ID ticket e valore=numero di risposte non lette
     */
    public Map<Integer, Integer> getUnreadRepliesForPlayer(UUID playerUUID) {
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
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, playerUUID.toString());
            pstmt.setString(3, playerUUID.toString());
            
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
     * Ottiene la lista dei ticket che necessitano di risposta dallo staff
     * @return Lista di ticket che necessitano di risposta
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
                Ticket ticket = new Ticket(
                    rs.getInt("id"),
                    UUID.fromString(rs.getString("player_uuid")),
                    rs.getString("player_name"),
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
     * Ottiene i ticket chiusi che richiedono ancora feedback
     */
    public List<Ticket> getPendingFeedbackTickets(UUID playerUuid) {
        List<Ticket> pendingFeedbackTickets = new ArrayList<>();
        
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
                Ticket ticket = new Ticket(
                    rs.getInt("id"),
                    UUID.fromString(rs.getString("player_uuid")),
                    rs.getString("player_name"),
                    rs.getString("description"),
                    rs.getString("status"),
                    rs.getString("created_at")
                );
                pendingFeedbackTickets.add(ticket);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore nel recupero dei ticket in attesa di feedback: " + e.getMessage());
            e.printStackTrace();
        }
        
        return pendingFeedbackTickets;
    }
    
    /**
     * Aggiorna la visualizzazione del ticket per un giocatore
     * @param ticketId ID del ticket
     * @param player Giocatore che visualizza
     */
    public void markTicketAsViewed(int ticketId, Player player) {
        // Trova l'ID dell'ultima risposta
        int lastReplyId = 0;
        String sql = "SELECT MAX(id) as max_id FROM replies WHERE ticket_id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, ticketId);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                lastReplyId = rs.getInt("max_id");
            }
            
            // Aggiorna la vista
            String updateSql = "INSERT OR REPLACE INTO ticket_views " +
                              "(ticket_id, player_uuid, last_viewed_reply_id, last_viewed_at) " +
                              "VALUES (?, ?, ?, datetime('now'))";
                              
            try (PreparedStatement updateStmt = connection.prepareStatement(updateSql)) {
                updateStmt.setInt(1, ticketId);
                updateStmt.setString(2, player.getUniqueId().toString());
                updateStmt.setInt(3, lastReplyId);
                updateStmt.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore durante l'aggiornamento della visualizzazione ticket: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Ottiene ticket di un giocatore tramite nome
     * @param playerName Nome del giocatore 
     * @return Lista di ticket del giocatore
     */
    public List<Ticket> getPlayerTicketsByName(String playerName) {
        List<Ticket> tickets = new ArrayList<>();
        String sql = "SELECT * FROM tickets WHERE player_name = ? COLLATE NOCASE ORDER BY id DESC";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                Ticket ticket = new Ticket(
                    rs.getInt("id"),
                    UUID.fromString(rs.getString("player_uuid")),
                    rs.getString("player_name"),
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
     * Salva un feedback per un ticket chiuso
     */
    public boolean saveTicketFeedback(int ticketId, UUID playerUuid, int rating, String comment) {
        String sql = "INSERT OR REPLACE INTO ticket_feedback " +
                     "(ticket_id, player_uuid, rating, comment, created_at) " +
                     "VALUES (?, ?, ?, ?, datetime('now'))";
                     
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, ticketId);
            pstmt.setString(2, playerUuid.toString());
            pstmt.setInt(3, rating);
            pstmt.setString(4, comment != null ? comment : "");
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore nel salvataggio del feedback: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Recupera le statistiche dei ticket
     */
    public Map<String, Object> getTicketStats() {
        Map<String, Object> stats = new HashMap<>();
        
        try (Statement stmt = connection.createStatement()) {
            // Ticket totali
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM tickets");
            if (rs.next()) {
                stats.put("total_tickets", rs.getInt(1));
            }
            
            // Ticket aperti
            rs = stmt.executeQuery("SELECT COUNT(*) FROM tickets WHERE status = 'OPEN'");
            if (rs.next()) {
                stats.put("open_tickets", rs.getInt(1));
            }
            
            // Ticket chiusi
            rs = stmt.executeQuery("SELECT COUNT(*) FROM tickets WHERE status = 'CLOSED'");
            if (rs.next()) {
                stats.put("closed_tickets", rs.getInt(1));
            }
            
            // Tempo medio di prima risposta
            rs = stmt.executeQuery("SELECT AVG(first_response_seconds) FROM ticket_stats WHERE first_response_seconds > 0");
            if (rs.next()) {
                double avgResponseTime = rs.getDouble(1);
                stats.put("avg_response_time_seconds", avgResponseTime);
                stats.put("avg_response_time_formatted", formatTimeInterval(avgResponseTime));
            }
            
            // Tempo medio di risoluzione
            rs = stmt.executeQuery("SELECT AVG(resolution_seconds) FROM ticket_stats WHERE resolution_seconds > 0");
            if (rs.next()) {
                double avgResolutionTime = rs.getDouble(1);
                stats.put("avg_resolution_time_seconds", avgResolutionTime);
                stats.put("avg_resolution_time_formatted", formatTimeInterval(avgResolutionTime));
            }
            
            // Valutazione media
            rs = stmt.executeQuery("SELECT AVG(rating) FROM ticket_feedback WHERE rating > 0");
            if (rs.next()) {
                double avgRating = rs.getDouble(1);
                stats.put("avg_rating", Math.round(avgRating * 10.0) / 10.0); // Arrotonda a 1 decimale
            }
            
            // Distribuzione valutazioni
            Map<Integer, Integer> ratingDistribution = new HashMap<>();
            for (int i = 1; i <= 5; i++) {
                PreparedStatement pstmt = connection.prepareStatement(
                    "SELECT COUNT(*) FROM ticket_feedback WHERE rating = ?");
                pstmt.setInt(1, i);
                rs = pstmt.executeQuery();
                if (rs.next()) {
                    ratingDistribution.put(i, rs.getInt(1));
                } else {
                    ratingDistribution.put(i, 0);
                }
            }
            stats.put("rating_distribution", ratingDistribution);
            
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore nel recupero delle statistiche: " + e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * Formatta un intervallo di tempo in secondi in una stringa leggibile
     */
    private String formatTimeInterval(double seconds) {
        if (Double.isNaN(seconds)) return "N/A";
        
        long totalSeconds = (long) seconds;
        long days = totalSeconds / (24 * 3600);
        long hours = (totalSeconds % (24 * 3600)) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long remainingSeconds = totalSeconds % 60;
        
        StringBuilder result = new StringBuilder();
        if (days > 0) result.append(days).append("g ");
        if (hours > 0 || days > 0) result.append(hours).append("h ");
        if (minutes > 0 || hours > 0 || days > 0) result.append(minutes).append("m ");
        result.append(remainingSeconds).append("s");
        
        return result.toString();
    }

}
