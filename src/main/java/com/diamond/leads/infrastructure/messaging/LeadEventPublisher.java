package com.diamond.leads.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import com.diamond.leads.infrastructure.messaging.event.LeadAssignedEvent;
import com.diamond.leads.infrastructure.messaging.event.LeadClientResolveCommand;
import com.diamond.leads.infrastructure.messaging.event.LeadStatusChangedEvent;

/**
 * Responsável por publicar eventos e comandos no RabbitMQ.
 *
 * Padrões demonstrados:
 * - Command Message (publishClientResolveCommand)
 * - Event Notification (publishStatusChanged, publishLeadAssigned)
 */
@Component
public class LeadEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(LeadEventPublisher.class);
    private final RabbitTemplate rabbitTemplate;

    public LeadEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Command Message: solicita ao client-service que resolva (find-or-create) o cliente.
     * Exchange: lead.commands (Direct) / Routing Key: lead.client.resolve
     */
    public void publishClientResolveCommand(LeadClientResolveCommand command) {
        log.info("Publicando comando lead.client.resolve para lead={}, cnpj={}",
                command.leadId(), command.cnpj());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.LEAD_COMMANDS_EXCHANGE,
                RabbitMQConfig.RK_CLIENT_RESOLVE,
                command);
    }

    /**
     * Event Notification: notifica que o status de um lead foi alterado.
     * Exchange: lead.events (Topic) / Routing Key: lead.status-changed
     */
    public void publishStatusChanged(LeadStatusChangedEvent event) {
        log.info("Publicando evento lead.status-changed para lead={}, {} -> {}",
                event.leadId(), event.oldStatus(), event.newStatus());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.LEAD_EVENTS_EXCHANGE,
                RabbitMQConfig.RK_STATUS_CHANGED,
                event);
    }

    /**
     * Event Notification: notifica que um lead foi atribuído a um vendedor.
     * Exchange: lead.events (Topic) / Routing Key: lead.assigned
     */
    public void publishLeadAssigned(LeadAssignedEvent event) {
        log.info("Publicando evento lead.assigned para lead={}, assignedTo={}",
                event.leadId(), event.assignedTo());
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.LEAD_EVENTS_EXCHANGE,
                RabbitMQConfig.RK_ASSIGNED,
                event);
    }
}
