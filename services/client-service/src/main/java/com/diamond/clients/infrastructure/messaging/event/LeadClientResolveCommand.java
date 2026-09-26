package com.diamond.clients.infrastructure.messaging.event;

import java.util.UUID;

/**
 * Command Message: recebido do lead-service solicitando resolução de um cliente.
 */
public record LeadClientResolveCommand(
        UUID leadId,
        String cnpj,
        String name,
        String phone) {
}
