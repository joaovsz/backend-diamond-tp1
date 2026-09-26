package com.diamond.leads.infrastructure.messaging.event;

import java.util.UUID;

/**
 * Event Notification: notifica que um lead foi atribuído a um vendedor.
 * Padrão: Event Notification — comunica um fato ocorrido sem carregar todo o estado.
 */
public record LeadAssignedEvent(
        UUID leadId,
        String prefix,
        String assignedTo,
        String assignedBy,
        String timestamp) {
}
