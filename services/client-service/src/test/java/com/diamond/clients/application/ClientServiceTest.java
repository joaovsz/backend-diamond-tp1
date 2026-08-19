package com.diamond.clients.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.diamond.clients.application.dto.ClientCreateRequest;
import com.diamond.clients.application.dto.ClientFindOrCreateRequest;
import com.diamond.clients.application.dto.ClientResponse;
import com.diamond.clients.application.dto.ClientUpdateRequest;
import com.diamond.clients.domain.Client;
import com.diamond.clients.infrastructure.persistence.repository.ClientRepository;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private ClientService clientService;

    @Test
    void findById_throwsNotFound_whenClientDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(clientRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.findById(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void findByCnpj_throwsNotFound_whenClientDoesNotExist() {
        when(clientRepository.findByCnpj("00000000000000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.findByCnpj("00000000000000"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void create_persistsClientWithRequestFields() {
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClientResponse response = clientService.create(
                new ClientCreateRequest("00000000000000", "Cliente Teste", "11999990000", "Executivo", 3, "Observação"));

        assertThat(response.cnpj()).isEqualTo("00000000000000");
        assertThat(response.marketSegment()).isEqualTo("Executivo");
        assertThat(response.fleetSize()).isEqualTo(3);
    }

    @Test
    void update_changesEditableFieldsButNotCnpj() {
        Client existing = existingClient();
        when(clientRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(clientRepository.save(existing)).thenReturn(existing);

        ClientResponse response = clientService.update(existing.getId(),
                new ClientUpdateRequest("Novo Nome", "11888880000", "Cargueiro", 5, "Nova observação"));

        assertThat(response.cnpj()).isEqualTo(existing.getCnpj());
        assertThat(response.name()).isEqualTo("Novo Nome");
        assertThat(response.marketSegment()).isEqualTo("Cargueiro");
        assertThat(response.fleetSize()).isEqualTo(5);
    }

    @Test
    void findOrCreate_returnsExisting_whenCnpjAlreadyExists_withoutOverwritingCuratedFields() {
        Client existing = existingClient();
        existing.setMarketSegment("Executivo");
        existing.setFleetSize(2);
        when(clientRepository.findByCnpj(existing.getCnpj())).thenReturn(Optional.of(existing));

        ClientResponse response = clientService.findOrCreate(
                new ClientFindOrCreateRequest(existing.getCnpj(), "Outro Nome", "11000000000"));

        assertThat(response.marketSegment()).isEqualTo("Executivo");
        assertThat(response.fleetSize()).isEqualTo(2);
        verify(clientRepository, org.mockito.Mockito.never()).save(any(Client.class));
    }

    @Test
    void findOrCreate_createsNew_whenCnpjNotFound() {
        when(clientRepository.findByCnpj("44444444444444")).thenReturn(Optional.empty());
        when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClientResponse response = clientService.findOrCreate(
                new ClientFindOrCreateRequest("44444444444444", "Cliente Novo", "11977776666"));

        assertThat(response.cnpj()).isEqualTo("44444444444444");
        assertThat(response.marketSegment()).isNull();
    }

    @Test
    void delete_removesClient() {
        Client existing = existingClient();
        when(clientRepository.findById(existing.getId())).thenReturn(Optional.of(existing));

        clientService.delete(existing.getId());

        verify(clientRepository).delete(existing);
    }

    private Client existingClient() {
        Client client = new Client();
        client.setId(UUID.randomUUID());
        client.setCnpj("00000000000000");
        client.setName("Cliente Teste");
        client.setPhone("11999990000");
        client.setCreatedAt(LocalDateTime.now());
        return client;
    }
}
