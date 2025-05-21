package it.Gecky.gktickets.blacklist;

import java.util.UUID;

public class BlacklistEntry {
    private final UUID playerUUID;
    private final String playerName;
    private final String reason;
    private final UUID blacklistedBy;
    private final String blacklistedByName;
    private final String createdAt;
    private final String expiresAt;
    
    public BlacklistEntry(UUID playerUUID, String playerName, String reason, UUID blacklistedBy, 
                      String blacklistedByName, String createdAt, String expiresAt) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.reason = reason;
        this.blacklistedBy = blacklistedBy;
        this.blacklistedByName = blacklistedByName;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }
    
    public UUID getPlayerUUID() {
        return playerUUID;
    }
    
    public String getPlayerName() {
        return playerName;
    }
    
    public String getReason() {
        return reason;
    }
    
    public UUID getBlacklistedBy() {
        return blacklistedBy;
    }
    
    public String getBlacklistedByName() {
        return blacklistedByName;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public String getExpiresAt() {
        return expiresAt;
    }
    
    public boolean isPermanent() {
        return expiresAt == null;
    }
}
