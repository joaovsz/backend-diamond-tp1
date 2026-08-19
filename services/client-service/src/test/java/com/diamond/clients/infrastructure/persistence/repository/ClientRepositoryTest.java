package com.diamond.clients.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.diamond.clients.domain.Client;

@DataJpaTest
class ClientRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ClientRepository clientRepository;

    @Test
    void save_persistsClient() {
        Client saved = clientRepository.saveAndFlush(newClient("00000000000000"));
        entityManager.clear();

        Client found = clientRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getCnpj()).isEqualTo("00000000000000");
        assertThat(found.getName()).isEqualTo("Cliente Teste");
    }

    @Test
    void findByCnpj_returnsClient() {
        clientRepository.saveAndFlush(newClient("11111111111111"));

        Optional<Client> found = clientRepository.findByCnpj("11111111111111");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Cliente Teste");
    }

    @Test
    void findByCnpj_returnsEmptyForUnknownCnpj() {
        Optional<Client> found = clientRepository.findByCnpj("99999999999999");

        assertThat(found).isEmpty();
    }

    @Test
    void findById_returnsEmptyForUnknownId() {
        Optional<Client> found = clientRepository.findById(UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    @Test
    void findAll_returnsAllPersistedClients() {
        clientRepository.saveAndFlush(newClient("22222222222222"));
        clientRepository.saveAndFlush(newClient("33333333333333"));

        List<Client> all = clientRepository.findAll();

        assertThat(all).hasSize(2);
    }

    private Client newClient(String cnpj) {
        Client client = new Client();
        client.setCnpj(cnpj);
        client.setName("Cliente Teste");
        client.setPhone("11999990000");
        client.setCreatedAt(LocalDateTime.now());
        return client;
    }
}
