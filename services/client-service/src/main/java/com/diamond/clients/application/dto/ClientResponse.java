package com.diamond.clients.application.dto;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

import com.diamond.clients.domain.Client;

public record ClientResponse(
        UUID id,
        String cnpj,
        String name,
        String phone,
        String marketSegment,
        Integer fleetSize,
        String commercialNotes,
        String createdAt) {

    private static final DateTimeFormatter CREATED_AT_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static ClientResponse fromEntity(Client client) {
        return new ClientResponse(
                client.getId(),
                client.getCnpj(),
                client.getName(),
                client.getPhone(),
                client.getMarketSegment(),
                client.getFleetSize(),
                client.getCommercialNotes(),
                client.getCreatedAt().format(CREATED_AT_FORMAT));
    }
}
