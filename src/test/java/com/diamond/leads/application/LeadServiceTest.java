package com.diamond.leads.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.diamond.leads.application.dto.LeadAssignmentRequest;
import com.diamond.leads.application.dto.LeadCreateRequest;
import com.diamond.leads.application.dto.LeadHistoryRequest;
import com.diamond.leads.application.dto.LeadResponse;
import com.diamond.leads.domain.Lead;
import com.diamond.leads.domain.LeadClient;
import com.diamond.leads.domain.LeadHistoryEntry;
import com.diamond.leads.domain.LeadStatus;
import com.diamond.leads.infrastructure.persistence.repository.LeadHistoryRepository;
import com.diamond.leads.infrastructure.persistence.repository.LeadRepository;

@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private LeadHistoryRepository leadHistoryRepository;

    @InjectMocks
    private LeadService leadService;

    @Test
    void findById_throwsNotFound_whenLeadDoesNotExist() {
        UUID id = UUID.randomUUID();
        when(leadRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leadService.findById(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    void addHistory_appendsEntryAndMarksDailyCompleted() {
        Lead lead = existingLead();
        when(leadRepository.findById(lead.getId())).thenReturn(Optional.of(lead));
        when(leadRepository.save(lead)).thenReturn(lead);

        leadService.addHistory(lead.getId(), new LeadHistoryRequest("Ligar novamente", "Cliente pediu retorno"));

        assertThat(lead.getHistory()).hasSize(1);
        assertThat(lead.getHistory().get(0).getStatus()).isEqualTo("Ligar novamente");
        assertThat(lead.getHistory().get(0).getLead()).isSameAs(lead);
        assertThat(lead.isDailyCompleted()).isTrue();
        assertThat(lead.getStatus()).isEqualTo(LeadStatus.ATENDIMENTO);
    }

    @Test
    void addHistory_mapsFechadoStatus() {
        Lead lead = existingLead();
        when(leadRepository.findById(lead.getId())).thenReturn(Optional.of(lead));
        when(leadRepository.save(lead)).thenReturn(lead);

        leadService.addHistory(lead.getId(), new LeadHistoryRequest("Fechado", "Negocio fechado"));

        assertThat(lead.getStatus()).isEqualTo(LeadStatus.FECHADO);
    }

    @Test
    void assignLead_fallsBackToRenata_whenAssignedByBlank() {
        Lead lead = existingLead();
        when(leadRepository.findById(lead.getId())).thenReturn(Optional.of(lead));
        when(leadRepository.save(lead)).thenReturn(lead);

        leadService.assignLead(lead.getId(), new LeadAssignmentRequest("v3", "  "));

        assertThat(lead.getAssignedBy()).isEqualTo("renata");
        assertThat(lead.getAssignedTo()).isEqualTo("v3");
    }

    @Test
    void findHistoryByLeadId_returnsMappedHistory() {
        Lead lead = existingLead();
        LeadHistoryEntry entry = new LeadHistoryEntry(lead, "Primeiro contato", "Nota registrada");
        entry.setId(UUID.randomUUID());
        entry.setTimestamp(LocalDateTime.now());
        when(leadRepository.findById(lead.getId())).thenReturn(Optional.of(lead));
        when(leadHistoryRepository.findByLeadIdOrderByTimestampDesc(lead.getId())).thenReturn(List.of(entry));

        List<LeadResponse.HistoryResponse> history = leadService.findHistoryByLeadId(lead.getId());

        assertThat(history).hasSize(1);
        assertThat(history.get(0).status()).isEqualTo("Primeiro contato");
        assertThat(history.get(0).note()).isEqualTo("Nota registrada");
    }

    @ParameterizedTest
    @CsvSource({
            "7, 40, critico",
            "40, 7, critico",
            "8, 40, atencao",
            "30, 40, atencao",
            "31, 40, normal",
            "40, 40, normal"
    })
    void create_computesPriorityByNearestDate(int tboDaysFromNow, int cvaDaysFromNow, String expectedPriority) {
        when(leadRepository.save(any(Lead.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeadCreateRequest request = new LeadCreateRequest(
                "PP-TST", "TESTE 100", "TESTE",
                LocalDate.now().plusDays(tboDaysFromNow).toString(),
                LocalDate.now().plusDays(cvaDaysFromNow).toString(),
                new LeadCreateRequest.ClientRequest("00000000000000", "Cliente Teste", "11999990000"),
                null);

        LeadResponse response = leadService.create(request);

        assertThat(response.priority()).isEqualTo(expectedPriority);
    }

    private Lead existingLead() {
        Lead lead = new Lead();
        lead.setId(UUID.randomUUID());
        lead.setPrefix("PP-TST");
        lead.setModel("TESTE 100");
        lead.setTypeLabel("TESTE");
        lead.setTboDate(LocalDate.now().plusDays(10).toString());
        lead.setCvaDate(LocalDate.now().plusDays(20).toString());
        lead.setClient(new LeadClient("00000000000000", "Cliente Teste", "11999990000"));
        lead.setAssignedBy("renata");
        lead.setPriority("atencao");
        lead.setDailyCompleted(false);
        lead.setStatus(LeadStatus.NOVO);
        return lead;
    }
}
