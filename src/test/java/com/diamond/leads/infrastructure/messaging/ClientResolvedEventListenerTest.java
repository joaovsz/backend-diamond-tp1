package com.diamond.leads.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.diamond.leads.domain.Lead;
import com.diamond.leads.domain.LeadClient;
import com.diamond.leads.domain.LeadStatus;
import com.diamond.leads.infrastructure.messaging.event.ClientResolvedEvent;
import com.diamond.leads.infrastructure.persistence.repository.LeadRepository;

@ExtendWith(MockitoExtension.class)
class ClientResolvedEventListenerTest {

    @Mock
    private LeadRepository leadRepository;

    @InjectMocks
    private ClientResolvedEventListener listener;

    @Test
    void handleClientResolved_updatesLeadClientId() {
        UUID leadId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        Lead lead = new Lead();
        lead.setId(leadId);
        lead.setPrefix("PP-TST");
        lead.setClient(new LeadClient("12345678000199", "Teste", "11999999999"));
        lead.setStatus(LeadStatus.NOVO);

        when(leadRepository.findById(leadId)).thenReturn(Optional.of(lead));
        when(leadRepository.save(lead)).thenReturn(lead);

        listener.handleClientResolved(new ClientResolvedEvent(leadId, clientId, "12345678000199"));

        assertThat(lead.getClientId()).isEqualTo(clientId);
        verify(leadRepository).save(lead);
    }

    @Test
    void handleClientResolved_logsWarning_whenLeadNotFound() {
        UUID leadId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        when(leadRepository.findById(leadId)).thenReturn(Optional.empty());

        // Não deve lançar exceção
        listener.handleClientResolved(new ClientResolvedEvent(leadId, clientId, "12345678000199"));
    }
}
