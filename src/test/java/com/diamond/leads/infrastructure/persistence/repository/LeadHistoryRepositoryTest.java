package com.diamond.leads.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import com.diamond.leads.domain.Lead;
import com.diamond.leads.domain.LeadClient;
import com.diamond.leads.domain.LeadHistoryEntry;

@DataJpaTest
class LeadHistoryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private LeadRepository leadRepository;

    @Autowired
    private LeadHistoryRepository leadHistoryRepository;

    @Test
    void findByLeadIdOrderByTimestampDesc_returnsMostRecentFirst() {
        Lead lead = newLead();
        LeadHistoryEntry older = lead.addHistoryEntry("Primeiro contato", "Nota mais antiga");
        LeadHistoryEntry newer = lead.addHistoryEntry("Ligar novamente", "Nota mais recente");
        leadRepository.saveAndFlush(lead);

        LocalDateTime now = LocalDateTime.now();
        setTimestamp(older, now.minusDays(2));
        setTimestamp(newer, now.minusHours(1));
        entityManager.clear();

        List<LeadHistoryEntry> history = leadHistoryRepository.findByLeadIdOrderByTimestampDesc(lead.getId());

        assertThat(history).hasSize(2);
        assertThat(history.get(0).getNote()).isEqualTo("Nota mais recente");
        assertThat(history.get(1).getNote()).isEqualTo("Nota mais antiga");
    }

    @Test
    void findByStatusIgnoreCase_matchesRegardlessOfCase() {
        Lead lead = newLead();
        lead.addHistoryEntry("Fechado", "Negocio fechado com sucesso");
        leadRepository.saveAndFlush(lead);
        entityManager.clear();

        List<LeadHistoryEntry> found = leadHistoryRepository.findByStatusIgnoreCase("fechado");

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getNote()).isEqualTo("Negocio fechado com sucesso");
    }

    @Test
    void findByTimestampBetween_filtersByDateRange() {
        Lead lead = newLead();
        LeadHistoryEntry withinRange = lead.addHistoryEntry("Ligar novamente", "Dentro do periodo");
        LeadHistoryEntry outsideRange = lead.addHistoryEntry("Ligar novamente", "Fora do periodo");
        leadRepository.saveAndFlush(lead);

        LocalDateTime now = LocalDateTime.now();
        setTimestamp(withinRange, now.minusDays(1));
        setTimestamp(outsideRange, now.minusDays(30));
        entityManager.clear();

        List<LeadHistoryEntry> found = leadHistoryRepository.findByTimestampBetween(now.minusDays(5), now);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getNote()).isEqualTo("Dentro do periodo");
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

    private void setTimestamp(LeadHistoryEntry entry, LocalDateTime timestamp) {
        entityManager.getEntityManager()
                .createQuery("update LeadHistoryEntry h set h.timestamp = :timestamp where h.id = :id")
                .setParameter("timestamp", timestamp)
                .setParameter("id", entry.getId())
                .executeUpdate();
    }
}
