package com.diamond.leads.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "leads")
public class Lead {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false)
    private String prefix;

    @Column(nullable = false)
    private String model;

    @Column(nullable = false)
    private String typeLabel;

    @Column(nullable = false)
    private String tboDate;

    @Column(nullable = false)
    private String cvaDate;

    @Embedded
    private LeadClient client;

    @Column(nullable = true)
    private String assignedTo;

    @Column(nullable = false)
    private String assignedBy;

    @Column(nullable = false)
    private String priority;

    @Column(nullable = false)
    private boolean dailyCompleted;

    @ElementCollection
    private List<LeadHistoryEntry> history = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeadStatus status = LeadStatus.NOVO;

    public Lead() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getTypeLabel() {
        return typeLabel;
    }

    public void setTypeLabel(String typeLabel) {
        this.typeLabel = typeLabel;
    }

    public String getTboDate() {
        return tboDate;
    }

    public void setTboDate(String tboDate) {
        this.tboDate = tboDate;
    }

    public String getCvaDate() {
        return cvaDate;
    }

    public void setCvaDate(String cvaDate) {
        this.cvaDate = cvaDate;
    }

    public LeadClient getClient() {
        return client;
    }

    public void setClient(LeadClient client) {
        this.client = client;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(String assignedBy) {
        this.assignedBy = assignedBy;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public boolean isDailyCompleted() {
        return dailyCompleted;
    }

    public void setDailyCompleted(boolean dailyCompleted) {
        this.dailyCompleted = dailyCompleted;
    }

    public List<LeadHistoryEntry> getHistory() {
        return history;
    }

    public void setHistory(List<LeadHistoryEntry> history) {
        this.history = history;
    }

    public LeadStatus getStatus() {
        return status;
    }

    public void setStatus(LeadStatus status) {
        this.status = status;
    }
}