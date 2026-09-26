package com.diamond.leads.infrastructure.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.diamond.leads.infrastructure.messaging.event.LeadAssignedEvent;
import com.diamond.leads.infrastructure.messaging.event.LeadStatusChangedEvent;

/**
 * Listener genérico de notificações de eventos de lead.
 *
 * Demonstra o padrão Event Notification: consome todos os eventos publicados
 * no exchange lead.events (topic exchange com binding lead.#) e os processa
 * para fins de logging/auditoria/notificações.
 *
 * Em um sistema de produção, este listener poderia enviar e-mails, push
 * notifications, atualizar dashboards em tempo real, etc.
 */
@Component
public class LeadNotificationListener {

    private static final Logger log = LoggerFactory.getLogger(LeadNotificationListener.class);

    @RabbitListener(queues = RabbitMQConfig.LEAD_NOTIFICATIONS_QUEUE)
    public void handleLeadNotification(org.springframework.amqp.core.Message message) {
        String body = new String(message.getBody());
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();

        log.info("[NOTIFICAÇÃO] Evento recebido — routingKey={}, payload={}", routingKey, body);

        switch (routingKey) {
            case RabbitMQConfig.RK_STATUS_CHANGED ->
                log.info("[NOTIFICAÇÃO] Lead teve status alterado. Detalhes: {}", body);
            case RabbitMQConfig.RK_ASSIGNED ->
                log.info("[NOTIFICAÇÃO] Lead foi atribuído a um vendedor. Detalhes: {}", body);
            default ->
                log.info("[NOTIFICAÇÃO] Evento genérico de lead recebido: {}", body);
        }
    }
}
