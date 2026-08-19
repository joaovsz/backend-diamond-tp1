package com.diamond.leads.infrastructure.client.dto;

public record ClientFindOrCreateRequest(String cnpj, String name, String phone) {
}
