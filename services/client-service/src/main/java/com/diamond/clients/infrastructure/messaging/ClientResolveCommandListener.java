package com.diamond.clients.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.diamond.clients.application.IClientService;
import com.diamond.clients.application.dto.ClientFindOrCreateRequest;
import com.diamond.clients.application.dto.ClientResponse;
import com.diamond.clients.infrastructure.messaging.event.ClientResolvedEvent;
import com.diamond.clients.infrastructure.messaging.event.LeadClientResolveCommand;

/**
 * Consome comandos lead.client.resolve da fila e executa find-or-create.
 *
 * Após resolver o cliente, publica um evento client.resolved (padrão
 * Event-Carried State Transfer) contendo leadId + clientId para que o
 * lead-service atualize o lead sem precisar de uma chamada HTTP de retorno.
 */
@Component
public class ClientResolveCommandListener {

    private static final Logger log = LoggerFactory.getLogger(ClientResolveCommandListener.class);

    private final IClientService clientService;
    private final RabbitTemplate rabbitTemplate;

    public ClientResolveCommandListener(IClientService clientService, RabbitTemplate rabbitTemplate) {
        this.clientService = clientService;
        this.rabbitTemplate = rabbitTemplate;
    }

    @RabbitListener(queues = RabbitMQConfig.CLIENT_RESOLVE_QUEUE)
    @Transactional
    public void handleResolveCommand(LeadClientResolveCommand command) {
        log.info("Recebido comando lead.client.resolve: leadId={}, cnpj={}",
                command.leadId(), command.cnpj());

        try {
            ClientResponse client = clientService.findOrCreate(
                    new ClientFindOrCreateRequest(command.cnpj(), command.name(), command.phone()));

            // Event-Carried State Transfer: publica resposta com todos os dados necessários
            ClientResolvedEvent event = new ClientResolvedEvent(
                    command.leadId(), client.id(), client.cnpj());

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.CLIENT_EVENTS_EXCHANGE,
                    RabbitMQConfig.RK_CLIENT_RESOLVED,
                    event);

            log.info("Publicado evento client.resolved: leadId={}, clientId={}",
                    command.leadId(), client.id());
        } catch (Exception ex) {
            log.error("Erro ao processar comando lead.client.resolve para leadId={}: {}",
                    command.leadId(), ex.getMessage(), ex);
            throw ex;
        }
    }
}
