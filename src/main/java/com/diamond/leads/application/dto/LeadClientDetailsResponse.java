package com.diamond.leads.application.dto;

import com.diamond.leads.domain.LeadClient;
import com.diamond.leads.infrastructure.client.dto.ClientDto;

public record LeadClientDetailsResponse(
        String cnpj,
        String name,
        String phone,
        String marketSegment,
        Integer fleetSize,
        String commercialNotes,
        boolean enriched,
        String unavailableReason) {

    public static LeadClientDetailsResponse enriched(LeadClient cached, ClientDto dto) {
        return new LeadClientDetailsResponse(
                cached.getCnpj(),
                cached.getName(),
                cached.getPhone(),
                dto.marketSegment(),
                dto.fleetSize(),
                dto.commercialNotes(),
                true,
                null);
    }

    public static LeadClientDetailsResponse cachedOnly(LeadClient cached, String reason) {
        return new LeadClientDetailsResponse(
                cached.getCnpj(),
                cached.getName(),
                cached.getPhone(),
                null,
                null,
                null,
                false,
                reason);
    }
}
