package com.diamond.clients.presentation;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.diamond.clients.application.IClientService;
import com.diamond.clients.application.dto.ClientCreateRequest;
import com.diamond.clients.application.dto.ClientFindOrCreateRequest;
import com.diamond.clients.application.dto.ClientResponse;
import com.diamond.clients.application.dto.ClientUpdateRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/clients")
@CrossOrigin(origins = "http://localhost:5173")
@Validated
public class ClientController {

    private final IClientService clientService;

    public ClientController(IClientService clientService) {
        this.clientService = clientService;
    }

    @GetMapping
    public List<ClientResponse> list() {
        return clientService.findAll();
    }

    @GetMapping("/{id}")
    public ClientResponse getById(@PathVariable UUID id) {
        return clientService.findById(id);
    }

    @GetMapping("/by-cnpj/{cnpj}")
    public ClientResponse getByCnpj(@PathVariable String cnpj) {
        return clientService.findByCnpj(cnpj);
    }

    @PostMapping
    public ResponseEntity<ClientResponse> create(@Valid @RequestBody ClientCreateRequest request) {
        ClientResponse createdClient = clientService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdClient.id())
                .toUri();

        return ResponseEntity.created(location).body(createdClient);
    }

    @PutMapping("/{id}")
    public ClientResponse update(@PathVariable UUID id, @Valid @RequestBody ClientUpdateRequest request) {
        return clientService.update(id, request);
    }

    @PostMapping("/find-or-create")
    public ClientResponse findOrCreate(@Valid @RequestBody ClientFindOrCreateRequest request) {
        return clientService.findOrCreate(request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        clientService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
