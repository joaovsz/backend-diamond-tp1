package com.diamond.clients.application;

import java.util.List;
import java.util.UUID;

import com.diamond.clients.application.dto.ClientCreateRequest;
import com.diamond.clients.application.dto.ClientFindOrCreateRequest;
import com.diamond.clients.application.dto.ClientResponse;
import com.diamond.clients.application.dto.ClientUpdateRequest;

public interface IClientService {

    List<ClientResponse> findAll();

    ClientResponse findById(UUID id);

    ClientResponse findByCnpj(String cnpj);

    ClientResponse create(ClientCreateRequest request);

    ClientResponse update(UUID id, ClientUpdateRequest request);

    ClientResponse findOrCreate(ClientFindOrCreateRequest request);

    void delete(UUID id);
}
