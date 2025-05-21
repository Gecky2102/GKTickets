package it.Gecky.gktickets.models;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Ticket {
    
    private int id;
    private UUID playerUuid;
    private String playerName;
    private String description;
    private String status;
    private String createdAt;
    private List<Reply> replies;
    
    public Ticket(int id, UUID playerUuid, String playerName, String description, String status, String createdAt) {
        this.id = id;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.description = description;
        this.status = status;
        this.createdAt = createdAt;
        this.replies = new ArrayList<>();
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
