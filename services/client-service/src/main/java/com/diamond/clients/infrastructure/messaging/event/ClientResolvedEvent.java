package com.diamond.clients.infrastructure.messaging.event;

import java.util.UUID;

/**
 * Event-Carried State Transfer: publicado após resolução do cliente,
 * carrega leadId + clientId para que o lead-service atualize o lead.
 */
public record ClientResolvedEvent(
        UUID leadId,
        UUID clientId,
        String cnpj) {
}
