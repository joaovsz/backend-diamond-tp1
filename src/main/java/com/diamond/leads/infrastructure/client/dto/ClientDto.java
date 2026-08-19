package com.diamond.leads.infrastructure.client.dto;

import java.util.UUID;

public record ClientDto(
        UUID id,
        String cnpj,
        String name,
        String phone,
        String marketSegment,
        Integer fleetSize,
        String commercialNotes,
        String createdAt) {
}
