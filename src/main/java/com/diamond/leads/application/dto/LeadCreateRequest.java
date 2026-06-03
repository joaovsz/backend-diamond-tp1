package com.diamond.leads.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LeadCreateRequest(
        @NotBlank @Size(max = 20) String prefix,
        @NotBlank @Size(max = 80) String model,
        @NotBlank @Size(max = 80) String typeLabel,
        @NotBlank @Size(max = 10) String tboDate,
        @NotBlank @Size(max = 10) String cvaDate,
        @NotNull @Valid ClientRequest client,
        @Size(max = 40) String assignedTo) {

    public record ClientRequest(
            @NotBlank @Size(max = 14) String cnpj,
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 30) String phone) {
    }
}