package com.diamond.leads.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class LeadHistoryEntry {

    @Column(name = "history_status", nullable = false)
    private String status;

    @Column(name = "history_note", nullable = false, length = 1000)
    private String note;

    @Column(name = "history_timestamp", nullable = false)
    private String timestamp;

    public LeadHistoryEntry() {
    }

    public LeadHistoryEntry(String status, String note, String timestamp) {
        this.status = status;
        this.note = note;
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}