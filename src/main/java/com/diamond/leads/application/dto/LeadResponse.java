package com.diamond.leads.application.dto;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.diamond.leads.domain.Lead;

public record LeadResponse(
        UUID id,
        String prefix,
        String model,
        String typeLabel,
        String tboDate,
        String cvaDate,
        ClientResponse client,
        String assignedTo,
        String assignedBy,
        String priority,
        boolean dailyCompleted,
        List<HistoryResponse> history) {

    private static final DateTimeFormatter HISTORY_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static LeadResponse fromEntity(Lead lead) {
        return new LeadResponse(
                lead.getId(),
                lead.getPrefix(),
                lead.getModel(),
                lead.getTypeLabel(),
                lead.getTboDate(),
                lead.getCvaDate(),
                new ClientResponse(lead.getClient().getCnpj(), lead.getClient().getName(), lead.getClient().getPhone()),
                lead.getAssignedTo(),
                lead.getAssignedBy(),
                lead.getPriority(),
                lead.isDailyCompleted(),
                lead.getHistory().stream()
                        .map(entry -> new HistoryResponse(
                                entry.getId(),
                                entry.getStatus(),
                                entry.getNote(),
                                entry.getTimestamp().format(HISTORY_TIMESTAMP_FORMAT)))
                        .collect(Collectors.toList()));
    }

    public record ClientResponse(String cnpj, String name, String phone) {
    }

    public record HistoryResponse(UUID id, String status, String note, String timestamp) {
    }
}