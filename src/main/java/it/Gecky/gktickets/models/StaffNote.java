package it.Gecky.gktickets.models;

import java.util.UUID;

public class StaffNote {
    private final int id;
    private final int ticketId;
    private final UUID staffUuid;
    private final String staffName;
    private final String note;
    private final String createdAt;
    
    public StaffNote(int id, int ticketId, UUID staffUuid, String staffName, String note, String createdAt) {
        this.id = id;
        this.ticketId = ticketId;
        this.staffUuid = staffUuid;
        this.staffName = staffName;
        this.note = note;
        this.createdAt = createdAt;
    }
    
    public int getId() {
        return id;
    }
    
    public int getTicketId() {
        return ticketId;
    }
    
    public UUID getStaffUuid() {
        return staffUuid;
    }
    
    public String getStaffName() {
        return staffName;
    }
    
    public String getNote() {
        return note;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
}
