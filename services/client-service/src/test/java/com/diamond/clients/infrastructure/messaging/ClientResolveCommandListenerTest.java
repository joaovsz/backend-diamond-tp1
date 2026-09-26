package com.diamond.clients.infrastructure.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import com.diamond.clients.application.IClientService;
import com.diamond.clients.application.dto.ClientFindOrCreateRequest;
import com.diamond.clients.application.dto.ClientResponse;
import com.diamond.clients.infrastructure.messaging.event.ClientResolvedEvent;
import com.diamond.clients.infrastructure.messaging.event.LeadClientResolveCommand;

@ExtendWith(MockitoExtension.class)
class ClientResolveCommandListenerTest {

    @Mock
    private IClientService clientService;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private ClientResolveCommandListener listener;

    @Test
    void handleResolveCommand_findsOrCreatesClientAndPublishesEvent() {
        UUID leadId = UUID.randomUUID();
        UUID clientId = UUID.randomUUID();

        when(clientService.findOrCreate(any(ClientFindOrCreateRequest.class)))
                .thenReturn(new ClientResponse(clientId, "12345678000199", "Aeroclube Paulista",
                        "11999887766", null, null, null, "01/01/2026 10:00"));

        listener.handleResolveCommand(
                new LeadClientResolveCommand(leadId, "12345678000199", "Aeroclube Paulista", "11999887766"));

        verify(clientService).findOrCreate(any(ClientFindOrCreateRequest.class));
        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.CLIENT_EVENTS_EXCHANGE),
                eq(RabbitMQConfig.RK_CLIENT_RESOLVED),
                any(ClientResolvedEvent.class));
    }
}
