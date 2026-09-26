package com.diamond.leads.infrastructure.messaging.event;

import java.util.UUID;

/**
 * Command Message: solicita ao client-service que resolva (find-or-create) um cliente.
 * Padrão: Command Message — expressa uma intenção de ação, não um fato ocorrido.
 */
public record LeadClientResolveCommand(
        UUID leadId,
        String cnpj,
        String name,
        String phone) {
}
