package com.diamond.leads.application.dto;

import jakarta.validation.constraints.Size;

public record LeadAssignmentRequest(
        @Size(max = 40) String assignedTo,
        @Size(max = 40) String assignedBy) {
}