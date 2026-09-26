package com.diamond.leads.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import com.diamond.leads.application.dto.LeadAssignmentRequest;
import com.diamond.leads.application.dto.LeadClientDetailsResponse;
import com.diamond.leads.application.dto.LeadCreateRequest;
import com.diamond.leads.application.dto.LeadHistoryRequest;
import com.diamond.leads.application.dto.LeadResponse;
import com.diamond.leads.domain.Lead;
import com.diamond.leads.domain.LeadClient;
import com.diamond.leads.domain.LeadHistoryEntry;
import com.diamond.leads.domain.LeadStatus;
import com.diamond.leads.infrastructure.client.ClientIntegrationService;
import com.diamond.leads.infrastructure.client.dto.ClientDto;
import com.diamond.leads.infrastructure.messaging.LeadEventPublisher;
import com.diamond.leads.infrastructure.persistence.repository.LeadHistoryRepository;
import com.diamond.leads.infrastructure.persistence.repository.LeadRepository;

@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock
    private LeadRepository leadRepository;

    @Mock
    private LeadHistoryRepository leadHistoryRepository;

    @Mock
    private ClientIntegrationService clientIntegrationService;

    @Mock
    private LeadEventPublisher leadEventPublisher;

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

    @Test
    void create_publishesClientResolveCommand() {
        when(leadRepository.save(any(Lead.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeadCreateRequest request = new LeadCreateRequest(
                "PP-TST", "TESTE 100", "TESTE",
                LocalDate.now().plusDays(10).toString(),
                LocalDate.now().plusDays(20).toString(),
                new LeadCreateRequest.ClientRequest("00000000000000", "Cliente Teste", "11999990000"),
                null);

        leadService.create(request);

        verify(leadEventPublisher).publishClientResolveCommand(
                argThat(cmd -> cmd.cnpj().equals("00000000000000")
                        && cmd.name().equals("Cliente Teste")
                        && cmd.phone().equals("11999990000")));
    }

    @Test
    void create_doesNotSetClientIdSynchronously() {
        when(leadRepository.save(any(Lead.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeadCreateRequest request = new LeadCreateRequest(
                "PP-TST", "TESTE 100", "TESTE",
                LocalDate.now().plusDays(10).toString(),
                LocalDate.now().plusDays(20).toString(),
                new LeadCreateRequest.ClientRequest("00000000000000", "Cliente Teste", "11999990000"),
                null);

        LeadResponse response = leadService.create(request);

        // clientId é preenchido assincronamente via evento client.resolved
        assertThat(response.clientId()).isNull();
    }

    @Test
    void addHistory_publishesStatusChangedEvent() {
        Lead lead = existingLead();
        when(leadRepository.findById(lead.getId())).thenReturn(Optional.of(lead));
        when(leadRepository.save(lead)).thenReturn(lead);

        leadService.addHistory(lead.getId(), new LeadHistoryRequest("Fechado", "Negócio fechado"));

        verify(leadEventPublisher).publishStatusChanged(
                argThat(event -> event.leadId().equals(lead.getId())
                        && event.oldStatus().equals("NOVO")
                        && event.newStatus().equals("FECHADO")));
    }

    @Test
    void assignLead_publishesLeadAssignedEvent() {
        Lead lead = existingLead();
        when(leadRepository.findById(lead.getId())).thenReturn(Optional.of(lead));
        when(leadRepository.save(lead)).thenReturn(lead);

        leadService.assignLead(lead.getId(), new LeadAssignmentRequest("v3", "renata"));

        verify(leadEventPublisher).publishLeadAssigned(
                argThat(event -> event.leadId().equals(lead.getId())
                        && event.assignedTo().equals("v3")
                        && event.assignedBy().equals("renata")));
    }

    @Test
    void findClientDetails_returnsEnrichedData_whenClientServiceAvailable() {
        Lead lead = existingLead();
        UUID clientId = UUID.randomUUID();
        lead.setClientId(clientId);
        when(leadRepository.findById(lead.getId())).thenReturn(Optional.of(lead));
        when(clientIntegrationService.findClientById(clientId))
                .thenReturn(Optional.of(new ClientDto(clientId, "00000000000000", "Cliente Teste",
                        "11999990000", "Executivo", 5, "Cliente estratégico", "01/01/2026 10:00")));

        LeadClientDetailsResponse response = leadService.findClientDetails(lead.getId());

        assertThat(response.enriched()).isTrue();
        assertThat(response.unavailableReason()).isNull();
        assertThat(response.marketSegment()).isEqualTo("Executivo");
        assertThat(response.fleetSize()).isEqualTo(5);
        assertThat(response.cnpj()).isEqualTo("00000000000000");
    }

    @Test
    void findClientDetails_returnsCachedOnlyWithReason_whenLeadHasNoClientId() {
        Lead lead = existingLead();
        when(leadRepository.findById(lead.getId())).thenReturn(Optional.of(lead));

        LeadClientDetailsResponse response = leadService.findClientDetails(lead.getId());

        assertThat(response.enriched()).isFalse();
        assertThat(response.unavailableReason()).isEqualTo("SEM_CLIENT_ID");
        assertThat(response.marketSegment()).isNull();
        assertThat(response.cnpj()).isEqualTo("00000000000000");
    }

    @Test
    void findClientDetails_returnsCachedOnlyWithReason_whenClientServiceUnavailable() {
        Lead lead = existingLead();
        UUID clientId = UUID.randomUUID();
        lead.setClientId(clientId);
        when(leadRepository.findById(lead.getId())).thenReturn(Optional.of(lead));
        when(clientIntegrationService.findClientById(clientId)).thenReturn(Optional.empty());

        LeadClientDetailsResponse response = leadService.findClientDetails(lead.getId());

        assertThat(response.enriched()).isFalse();
        assertThat(response.unavailableReason()).isEqualTo("CLIENT_SERVICE_INDISPONIVEL");
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
