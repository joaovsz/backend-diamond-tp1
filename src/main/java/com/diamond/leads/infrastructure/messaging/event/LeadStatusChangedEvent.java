package com.diamond.leads.infrastructure.messaging.event;

import java.util.UUID;

/**
 * Event Notification: notifica que o status de um lead foi alterado.
 * Padrão: Event Notification — comunica um fato ocorrido sem carregar todo o estado.
 */
public record LeadStatusChangedEvent(
        UUID leadId,
        String prefix,
        String oldStatus,
        String newStatus,
        String timestamp) {
}
