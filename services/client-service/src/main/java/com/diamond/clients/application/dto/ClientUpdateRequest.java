package com.diamond.clients.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClientUpdateRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 30) String phone,
        @Size(max = 40) String marketSegment,
        Integer fleetSize,
        @Size(max = 1000) String commercialNotes) {
}
