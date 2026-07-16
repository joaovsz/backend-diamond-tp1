package com.diamond.leads.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.diamond.leads.domain.Lead;
import com.diamond.leads.domain.LeadClient;

@DataJpaTest
class LeadRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private LeadHistoryRepository leadHistoryRepository;

    @Test
    void save_persistsLeadWithEmbeddedClient() {
        Lead saved = leadRepository.saveAndFlush(newLead());
        entityManager.clear();

        Lead found = leadRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getPrefix()).isEqualTo("PP-TST");
        assertThat(found.getClient().getCnpj()).isEqualTo("00000000000000");
        assertThat(found.getClient().getName()).isEqualTo("Cliente Teste");
    }

    @Test
    void findById_returnsEmptyForUnknownId() {
        Optional<Lead> found = leadRepository.findById(UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    @Test
    void findAll_returnsAllPersistedLeads() {
        leadRepository.saveAndFlush(newLead());
        leadRepository.saveAndFlush(newLead());

        List<Lead> all = leadRepository.findAll();

        assertThat(all).hasSize(2);
    }

    @Test
    void delete_removesAssociatedHistoryEntries() {
        Lead lead = newLead();
        lead.addHistoryEntry("Primeiro contato", "Nota de historico");
        Lead saved = leadRepository.saveAndFlush(lead);
        UUID leadId = saved.getId();
        assertThat(leadHistoryRepository.findByLeadIdOrderByTimestampDesc(leadId)).hasSize(1);

        leadRepository.delete(saved);
        leadRepository.flush();

        assertThat(leadHistoryRepository.findByLeadIdOrderByTimestampDesc(leadId)).isEmpty();
    }

    private Lead newLead() {
        Lead lead = new Lead();
        lead.setPrefix("PP-TST");
        lead.setModel("TESTE 100");
        lead.setTypeLabel("TESTE");
        lead.setTboDate("2026-12-01");
        lead.setCvaDate("2026-12-15");
        lead.setClient(new LeadClient("00000000000000", "Cliente Teste", "11999990000"));
        lead.setAssignedBy("renata");
        lead.setPriority("normal");
        lead.setDailyCompleted(false);
        return lead;
    }
}
