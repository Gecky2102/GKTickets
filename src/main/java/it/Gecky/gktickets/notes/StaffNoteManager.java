package it.Gecky.gktickets.notes;

import it.Gecky.gktickets.GKTickets;
import it.Gecky.gktickets.models.StaffNote;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class StaffNoteManager {

    private final GKTickets plugin;
    private final Connection connection;

    public StaffNoteManager(GKTickets plugin) {
        this.plugin = plugin;
        this.connection = plugin.getDatabaseManager().getConnection();
        
        // Ensure table exists
        createNotesTable();
    }
    
    /**
     * Creates the staff_notes table if it doesn't exist
     */
    private void createNotesTable() {
        try (PreparedStatement statement = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS staff_notes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "ticket_id INTEGER NOT NULL, " +
                "staff_uuid VARCHAR(36) NOT NULL, " +
                "staff_name VARCHAR(16) NOT NULL, " +
                "note TEXT NOT NULL, " +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (ticket_id) REFERENCES tickets(id))"
        )) {
            statement.execute();
        } catch (SQLException e) {
            plugin.getLogger().severe("Error creating staff_notes table: " + e.getMessage());
        }
    }
    
    /**
     * Add a staff note to a ticket
     * @param ticketId The ticket ID
     * @param staff The staff member
     * @param note The note content
     * @return true if successful
     */
    public boolean addNote(int ticketId, Player staff, String note) {
        String sql = "INSERT INTO staff_notes (ticket_id, staff_uuid, staff_name, note, created_at) VALUES (?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, ticketId);
            pstmt.setString(2, staff.getUniqueId().toString());
            pstmt.setString(3, staff.getName());
            pstmt.setString(4, note);
            pstmt.setString(5, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("Error adding staff note: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Get all notes for a ticket
     * @param ticketId The ticket ID
     * @return List of StaffNote objects
     */
    public List<StaffNote> getNotesForTicket(int ticketId) {
        List<StaffNote> notes = new ArrayList<>();
        String sql = "SELECT * FROM staff_notes WHERE ticket_id = ? ORDER BY created_at ASC";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, ticketId);
            ResultSet rs = pstmt.executeQuery();
            
            while (rs.next()) {
                StaffNote note = new StaffNote(
                    rs.getInt("id"),
                    rs.getInt("ticket_id"),
                    UUID.fromString(rs.getString("staff_uuid")),
                    rs.getString("staff_name"),
                    rs.getString("note"),
                    rs.getString("created_at")
                );
                notes.add(note);
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error retrieving staff notes: " + e.getMessage());
            e.printStackTrace();
        }
        
        return notes;
    }
}
