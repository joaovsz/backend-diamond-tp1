package com.diamond.clients.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.diamond.clients.domain.Client;

public interface ClientRepository extends JpaRepository<Client, UUID> {

    Optional<Client> findByCnpj(String cnpj);
}
