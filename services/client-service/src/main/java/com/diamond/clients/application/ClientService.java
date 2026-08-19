package com.diamond.clients.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.diamond.clients.application.dto.ClientCreateRequest;
import com.diamond.clients.application.dto.ClientFindOrCreateRequest;
import com.diamond.clients.application.dto.ClientResponse;
import com.diamond.clients.application.dto.ClientUpdateRequest;
import com.diamond.clients.domain.Client;
import com.diamond.clients.infrastructure.persistence.repository.ClientRepository;

@Service
@Transactional
public class ClientService implements IClientService {

    private final ClientRepository clientRepository;

    public ClientService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientResponse> findAll() {
        return clientRepository.findAll().stream()
                .map(ClientResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponse findById(UUID id) {
        return ClientResponse.fromEntity(getClientOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ClientResponse findByCnpj(String cnpj) {
        Client client = clientRepository.findByCnpj(cnpj)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado"));
        return ClientResponse.fromEntity(client);
    }

    @Override
    public ClientResponse create(ClientCreateRequest request) {
        Client client = new Client();
        client.setCnpj(request.cnpj());
        client.setName(request.name());
        client.setPhone(request.phone());
        client.setMarketSegment(request.marketSegment());
        client.setFleetSize(request.fleetSize());
        client.setCommercialNotes(request.commercialNotes());
        client.setCreatedAt(LocalDateTime.now());

        return ClientResponse.fromEntity(clientRepository.save(client));
    }

    @Override
    public ClientResponse update(UUID id, ClientUpdateRequest request) {
        Client client = getClientOrThrow(id);
        client.setName(request.name());
        client.setPhone(request.phone());
        client.setMarketSegment(request.marketSegment());
        client.setFleetSize(request.fleetSize());
        client.setCommercialNotes(request.commercialNotes());

        return ClientResponse.fromEntity(clientRepository.save(client));
    }

    @Override
    public ClientResponse findOrCreate(ClientFindOrCreateRequest request) {
        return clientRepository.findByCnpj(request.cnpj())
                .map(ClientResponse::fromEntity)
                .orElseGet(() -> {
                    Client client = new Client();
                    client.setCnpj(request.cnpj());
                    client.setName(request.name());
                    client.setPhone(request.phone());
                    client.setCreatedAt(LocalDateTime.now());
                    return ClientResponse.fromEntity(clientRepository.save(client));
                });
    }

    @Override
    public void delete(UUID id) {
        Client client = getClientOrThrow(id);
        clientRepository.delete(client);
    }

    private Client getClientOrThrow(UUID id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado"));
    }
}
