package it.giankoyt.gktickets.models;

import java.util.UUID;

public class Reply {
    
    private int id;
    private int ticketId;
    private UUID playerUuid;
    private String playerName;
    private String message;
    private String createdAt;
    
    public Reply(int id, int ticketId, UUID playerUuid, String playerName, String message, String createdAt) {
        this.id = id;
        this.ticketId = ticketId;
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.message = message;
        this.createdAt = createdAt;
    }

    public int getId() {
        return id;
    }

    public int getTicketId() {
        return ticketId;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getMessage() {
        return message;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
