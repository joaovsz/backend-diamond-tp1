package com.diamond.leads.infrastructure.client;

import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.diamond.leads.infrastructure.client.dto.ClientDto;
import com.diamond.leads.infrastructure.client.dto.ClientFindOrCreateRequest;

@Component
public class ClientIntegrationService {

    private static final Logger log = LoggerFactory.getLogger(ClientIntegrationService.class);

    private final ClientServiceClient clientServiceClient;

    public ClientIntegrationService(ClientServiceClient clientServiceClient) {
        this.clientServiceClient = clientServiceClient;
    }

    public Optional<ClientDto> findOrCreateClient(String cnpj, String name, String phone) {
        try {
            return Optional.ofNullable(clientServiceClient.findOrCreate(new ClientFindOrCreateRequest(cnpj, name, phone)));
        } catch (Exception ex) {
            log.warn("client-service indisponível ao resolver cliente cnpj={}: {}", cnpj, ex.getMessage());
            return Optional.empty();
        }
    }

    public Optional<ClientDto> findClientById(UUID clientId) {
        if (clientId == null) {
            return Optional.empty();
        }

        try {
            return Optional.ofNullable(clientServiceClient.getById(clientId));
        } catch (Exception ex) {
            log.warn("client-service indisponível ao buscar cliente id={}: {}", clientId, ex.getMessage());
            return Optional.empty();
        }
    }
}
