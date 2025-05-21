package it.Gecky.gktickets.models;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Ticket {
    private final int id;
    private final UUID playerUuid;
    private final String playerName;
    private final String description;
    private final String status;
    private final String createdAt;
    private final String category; // New field for category
    private List<Reply> replies;

    public Ticket(int id, UUID playerUuid, String playerName, String description, String status, String createdAt) {
        this(id, playerUuid, playerName, "general", description, status, createdAt);
    }
    
    public Ticket(int id, UUID playerUuid, String playerName, String category, String description, String status, String createdAt) {
        this.id = id;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.category = category;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.replies = new ArrayList<>();
    }

    /**
     * Constructor for testing and demo purposes
     */
    public Ticket(int id, UUID playerUuid, String playerName, String description, String status) {
        this(id, playerUuid, playerName, "general", description, status, 
             LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
    }

    public int getId() {
        return id;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getCategory() {
        return category;
    }

    public List<Reply> getReplies() {
        return replies;
    }

    public void setReplies(List<Reply> replies) {
        this.replies = replies;
    }
    
    public boolean isOpen() {
        return "OPEN".equalsIgnoreCase(status);
    }
}
