package com.diamond.clients.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "clients")
public class Client {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(nullable = false, unique = true)
    private String cnpj;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String phone;

    @Column(nullable = true)
    private String marketSegment;

    @Column(nullable = true)
    private Integer fleetSize;

    @Column(length = 1000, nullable = true)
    private String commercialNotes;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public Client() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getMarketSegment() {
        return marketSegment;
    }

    public void setMarketSegment(String marketSegment) {
        this.marketSegment = marketSegment;
    }

    public Integer getFleetSize() {
        return fleetSize;
    }

    public void setFleetSize(Integer fleetSize) {
        this.fleetSize = fleetSize;
    }

    public String getCommercialNotes() {
        return commercialNotes;
    }

    public void setCommercialNotes(String commercialNotes) {
        this.commercialNotes = commercialNotes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
