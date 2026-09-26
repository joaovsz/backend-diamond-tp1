package com.diamond.leads.infrastructure.messaging.event;

import java.util.UUID;

/**
 * Event-Carried State Transfer: carrega todos os dados necessários para que o
 * consumidor atualize o lead sem precisar de uma query adicional ao client-service.
 */
public record ClientResolvedEvent(
        UUID leadId,
        UUID clientId,
        String cnpj) {
}
