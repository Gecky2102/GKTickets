package it.Gecky.gktickets.blacklist;

import it.Gecky.gktickets.GKTickets;
import org.bukkit.OfflinePlayer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BlacklistManager {
    
    private final GKTickets plugin;
    private final Connection connection;
    private final Map<UUID, BlacklistEntry> blacklistedUsers;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public BlacklistManager(GKTickets plugin) {
        this.plugin = plugin;
        this.connection = plugin.getDatabaseManager().getConnection();
        this.blacklistedUsers = new HashMap<>();
        
        // Create the blacklist table if it doesn't exist
        createBlacklistTable();
        
        // Load all blacklisted users
        loadBlacklistedUsers();
    }
    
    /**
     * Creates the blacklist table if it doesn't exist
     */
    private void createBlacklistTable() {
        try (PreparedStatement statement = connection.prepareStatement(
                "CREATE TABLE IF NOT EXISTS blacklisted_users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "player_uuid VARCHAR(36) NOT NULL UNIQUE, " +
                "player_name VARCHAR(16) NOT NULL, " +
                "reason TEXT NOT NULL, " +
                "blacklisted_by VARCHAR(36) NOT NULL, " +
                "blacklisted_by_name VARCHAR(16) NOT NULL, " +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "expires_at DATETIME DEFAULT NULL)"
        )) {
            statement.execute();
            plugin.getLogger().info("Blacklist table created or verified successfully");
        } catch (SQLException e) {
            plugin.getLogger().severe("Error creating blacklist table: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Load all blacklisted users from the database
     */
    private void loadBlacklistedUsers() {
        blacklistedUsers.clear();
        try (PreparedStatement stmt = connection.prepareStatement(
                "SELECT * FROM blacklisted_users")) {
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                UUID playerUUID = UUID.fromString(rs.getString("player_uuid"));
                String playerName = rs.getString("player_name");
                String reason = rs.getString("reason");
                UUID blacklistedBy = UUID.fromString(rs.getString("blacklisted_by"));
                String blacklistedByName = rs.getString("blacklisted_by_name");
                String createdAt = rs.getString("created_at");
                String expiresAt = rs.getString("expires_at");
                
                BlacklistEntry entry = new BlacklistEntry(
                    playerUUID, 
                    playerName,
                    reason,
                    blacklistedBy,
                    blacklistedByName,
                    createdAt,
                    expiresAt
                );
                
                // Only add if still valid
                if (!isExpired(entry)) {
                    blacklistedUsers.put(playerUUID, entry);
                }
            }
            
            plugin.getLogger().info("Loaded " + blacklistedUsers.size() + " blacklisted users");
        } catch (SQLException e) {
            plugin.getLogger().severe("Error loading blacklisted users: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Check if a blacklist entry has expired
     * @param entry The entry to check
     * @return true if expired
     */
    private boolean isExpired(BlacklistEntry entry) {
        if (entry.getExpiresAt() == null) {
            return false; // No expiration = permanent
        }
        
        LocalDateTime expiryDate = LocalDateTime.parse(entry.getExpiresAt(), formatter);
        return LocalDateTime.now().isAfter(expiryDate);
    }
    
    /**
     * Check if a player is blacklisted
     * @param playerUUID The player's UUID
     * @return true if blacklisted
     */
    public boolean isBlacklisted(UUID playerUUID) {
        BlacklistEntry entry = blacklistedUsers.get(playerUUID);
        if (entry == null) {
            return false;
        }
        
        // Check for expiration
        if (isExpired(entry)) {
            blacklistedUsers.remove(playerUUID);
            removeFromDatabase(playerUUID);
            return false;
        }
        
        return true;
    }
    
    /**
     * Get the reason why a player is blacklisted
     * @param playerUUID The player's UUID
     * @return The blacklist reason or null if not blacklisted
     */
    public String getBlacklistReason(UUID playerUUID) {
        BlacklistEntry entry = blacklistedUsers.get(playerUUID);
        return entry != null ? entry.getReason() : null;
    }
    
    /**
     * Add a player to the blacklist
     * @param playerUUID Player's UUID
     * @param playerName Player's name
     * @param reason Reason for blacklisting
     * @param staffUUID Staff member's UUID
     * @param staffName Staff member's name
     * @param expiryDays Number of days until expiry (0 for permanent)
     * @return true if successful
     */
    public boolean blacklistPlayer(UUID playerUUID, String playerName, String reason, 
                                UUID staffUUID, String staffName, int expiryDays) {
        
        String expiresAt = null;
        if (expiryDays > 0) {
            LocalDateTime expiryDate = LocalDateTime.now().plusDays(expiryDays);
            expiresAt = expiryDate.format(formatter);
        }
        
        String createdAt = LocalDateTime.now().format(formatter);
        
        // First, remove any existing blacklist for this player
        removeFromDatabase(playerUUID);
        
        // Now add the new blacklist
        String sql = "INSERT INTO blacklisted_users (player_uuid, player_name, reason, blacklisted_by, " +
                     "blacklisted_by_name, created_at, expires_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            pstmt.setString(2, playerName);
            pstmt.setString(3, reason);
            pstmt.setString(4, staffUUID.toString());
            pstmt.setString(5, staffName);
            pstmt.setString(6, createdAt);
            pstmt.setString(7, expiresAt);
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                // Update the in-memory cache
                BlacklistEntry entry = new BlacklistEntry(
                    playerUUID,
                    playerName,
                    reason,
                    staffUUID,
                    staffName,
                    createdAt,
                    expiresAt
                );
                blacklistedUsers.put(playerUUID, entry);
                
                // Report to Discord if enabled
                plugin.getReportManager().reportBlacklist(playerName, reason, staffName, expiryDays);
                
                return true;
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Error blacklisting player: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Remove a player from the blacklist
     * @param playerUUID Player's UUID
     * @return true if successful
     */
    public boolean unblacklistPlayer(UUID playerUUID) {
        BlacklistEntry entry = blacklistedUsers.get(playerUUID);
        if (entry == null) {
            return false;
        }
        
        if (removeFromDatabase(playerUUID)) {
            blacklistedUsers.remove(playerUUID);
            
            // Report to Discord if enabled
            plugin.getReportManager().reportUnblacklist(entry.getPlayerName());
            
            return true;
        }
        
        return false;
    }
    
    /**
     * Remove a player from the blacklist database
     * @param playerUUID Player's UUID
     * @return true if successful
     */
    private boolean removeFromDatabase(UUID playerUUID) {
        String sql = "DELETE FROM blacklisted_users WHERE player_uuid = ?";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            plugin.getLogger().severe("Error removing player from blacklist: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Get all blacklisted players
     * @return List of blacklist entries
     */
    public List<BlacklistEntry> getBlacklistedPlayers() {
        return new ArrayList<>(blacklistedUsers.values());
    }
    
    /**
     * Get how many days are left on a blacklist
     * @param playerUUID Player's UUID
     * @return Days left or -1 if permanent, 0 if not blacklisted
     */
    public int getDaysRemaining(UUID playerUUID) {
        BlacklistEntry entry = blacklistedUsers.get(playerUUID);
        if (entry == null) {
            return 0;
        }
        
        if (entry.getExpiresAt() == null) {
            return -1; // Permanent
        }
        
        LocalDateTime expiryDate = LocalDateTime.parse(entry.getExpiresAt(), formatter);
        LocalDateTime now = LocalDateTime.now();
        
        // Calculate days between
        long daysUntil = java.time.temporal.ChronoUnit.DAYS.between(now, expiryDate);
        return (int) daysUntil;
    }
    
    /**
     * Reload blacklisted users from the database
     */
    public void reload() {
        loadBlacklistedUsers();
    }
}
