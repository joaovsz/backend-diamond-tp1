package com.diamond.leads.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LeadHistoryRequest(
        @NotBlank @Size(max = 40) String status,
        @NotBlank @Size(max = 1000) String note) {
}