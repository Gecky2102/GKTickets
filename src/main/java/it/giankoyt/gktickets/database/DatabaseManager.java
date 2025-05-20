package it.giankoyt.gktickets.database;

import it.giankoyt.gktickets.GKTickets;
import it.giankoyt.gktickets.models.Reply;
import it.giankoyt.gktickets.models.Ticket;
import org.bukkit.entity.Player;

import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
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
            
            plugin.getLogger().info("Tabelle del database create o verificate con successo.");
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore durante la creazione delle tabelle: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public int createTicket(Player player, String description) {
        String sql = "INSERT INTO tickets (player_uuid, player_name, description) VALUES (?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, player.getUniqueId().toString());
            pstmt.setString(2, player.getName());
            pstmt.setString(3, description);
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore durante la creazione del ticket: " + e.getMessage());
            e.printStackTrace();
        }
        
        return -1;
    }
    
    public List<Ticket> getOpenTickets() {
        List<Ticket> tickets = new ArrayList<>();
        String sql = "SELECT * FROM tickets WHERE status = 'OPEN' ORDER BY id DESC";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                tickets.add(extractTicketFromResultSet(rs));
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore durante il recupero dei ticket aperti: " + e.getMessage());
            e.printStackTrace();
        }
        
        return tickets;
    }
    
    public List<Ticket> getPlayerTickets(UUID playerUUID) {
        List<Ticket> tickets = new ArrayList<>();
        String sql = "SELECT * FROM tickets WHERE player_uuid = ? ORDER BY id DESC";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    tickets.add(extractTicketFromResultSet(rs));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore durante il recupero dei ticket del giocatore: " + e.getMessage());
            e.printStackTrace();
        }
        
        return tickets;
    }
    
    public Ticket getTicketById(int ticketId) {
        String sql = "SELECT * FROM tickets WHERE id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, ticketId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Ticket ticket = extractTicketFromResultSet(rs);
                    ticket.setReplies(getRepliesForTicket(ticketId));
                    return ticket;
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore durante il recupero del ticket con ID " + ticketId + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        return null;
    }
    
    public boolean closeTicket(int ticketId) {
        String sql = "UPDATE tickets SET status = 'CLOSED' WHERE id = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, ticketId);
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore durante la chiusura del ticket con ID " + ticketId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean addReply(int ticketId, Player player, String message) {
        String sql = "INSERT INTO replies (ticket_id, player_uuid, player_name, message) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, ticketId);
            pstmt.setString(2, player.getUniqueId().toString());
            pstmt.setString(3, player.getName());
            pstmt.setString(4, message);
            
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore durante l'aggiunta della risposta al ticket con ID " + ticketId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    public List<Reply> getRepliesForTicket(int ticketId) {
        List<Reply> replies = new ArrayList<>();
        String sql = "SELECT * FROM replies WHERE ticket_id = ? ORDER BY created_at ASC";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, ticketId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
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
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore durante il recupero delle risposte per il ticket con ID " + ticketId + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        return replies;
    }
    
    public int countOpenTicketsForUser(UUID playerUUID) {
        String sql = "SELECT COUNT(*) FROM tickets WHERE player_uuid = ? AND status = 'OPEN'";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Errore durante il conteggio dei ticket aperti per l'utente: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0;
    }
    
    private Ticket extractTicketFromResultSet(ResultSet rs) throws SQLException {
        return new Ticket(
            rs.getInt("id"),
            UUID.fromString(rs.getString("player_uuid")),
            rs.getString("player_name"),
            rs.getString("description"),
            rs.getString("status"),
            rs.getString("created_at")
        );
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
}
