package com.diamond.leads.infrastructure.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.diamond.leads.infrastructure.client.dto.ClientDto;
import com.diamond.leads.infrastructure.client.dto.ClientFindOrCreateRequest;

@FeignClient(name = "client-service")
public interface ClientServiceClient {

    @PostMapping("/api/clients/find-or-create")
    ClientDto findOrCreate(@RequestBody ClientFindOrCreateRequest request);

    @GetMapping("/api/clients/{id}")
    ClientDto getById(@PathVariable("id") UUID id);
}
