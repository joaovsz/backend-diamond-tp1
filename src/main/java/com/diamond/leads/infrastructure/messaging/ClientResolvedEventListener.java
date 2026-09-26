package com.diamond.leads.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.diamond.leads.infrastructure.messaging.event.ClientResolvedEvent;
import com.diamond.leads.infrastructure.persistence.repository.LeadRepository;

/**
 * Consome eventos client.resolved publicados pelo client-service.
 *
 * Padrão: Event-Carried State Transfer — o evento carrega todos os dados
 * necessários (leadId + clientId) para que o lead seja atualizado sem precisar
 * de uma query adicional ao client-service.
 */
@Component
public class ClientResolvedEventListener {

    private static final Logger log = LoggerFactory.getLogger(ClientResolvedEventListener.class);
    private final LeadRepository leadRepository;

    public ClientResolvedEventListener(LeadRepository leadRepository) {
        this.leadRepository = leadRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.LEAD_CLIENT_RESOLVED_QUEUE)
    @Transactional
    public void handleClientResolved(ClientResolvedEvent event) {
        log.info("Recebido evento client.resolved: leadId={}, clientId={}, cnpj={}",
                event.leadId(), event.clientId(), event.cnpj());

        leadRepository.findById(event.leadId()).ifPresentOrElse(
                lead -> {
                    lead.setClientId(event.clientId());
                    leadRepository.save(lead);
                    log.info("Lead {} atualizado com clientId={}", event.leadId(), event.clientId());
                },
                () -> log.warn("Lead {} não encontrado ao processar client.resolved", event.leadId())
        );
    }
}
