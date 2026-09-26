package com.diamond.leads.infrastructure.persistence;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.diamond.leads.domain.Lead;
import com.diamond.leads.domain.LeadClient;
import com.diamond.leads.infrastructure.messaging.LeadEventPublisher;
import com.diamond.leads.infrastructure.messaging.event.LeadClientResolveCommand;
import com.diamond.leads.infrastructure.persistence.repository.LeadRepository;

@Component
public class DataSeeder implements CommandLineRunner {

    private final LeadRepository leadRepository;
    private final LeadEventPublisher leadEventPublisher;

    public DataSeeder(LeadRepository leadRepository, LeadEventPublisher leadEventPublisher) {
        this.leadRepository = leadRepository;
        this.leadEventPublisher = leadEventPublisher;
    }

    @Override
    public void run(String... args) {
        if (leadRepository.count() > 0) {
            return;
        }

        List<Lead> seeds = List.of(
            createLead("PS-PLB", "SR22T", "CIRRUS / 1 ANO", "2026-06-12", "2026-07-03", "12345678000199", "Aeroclube Paulista", "11999887766", null, "automatico", "critico", false,
                "Primeiro contato", "Cliente pediu retorno para confirmar disponibilidade de hangar"),
            createLead("PR-GRY", "KING AIR C90", "KING AIR", "2026-07-12", "2026-08-05", "98765432000188", "Turbo Aviação", "21988776655", "v1", "renata", "atencao", false,
                "Ligar novamente", "Agenda comercial solicitou nova ligação na próxima semana"),
            createLead("PP-FLY", "208 CARAVAN", "CARAVAN", "2026-09-01", "2026-09-20", "55443322000177", "Fly Norte", "31990001122", "v2", "automatico", "normal", false,
                "Sem sucesso", "Telefone sem atendimento no primeiro disparo"),
            createLead("PT-JET", "PHENOM 300", "BIZJET", "2026-06-20", "2026-07-18", "10293847000156", "Jet Center Brasil", "1133224455", null, "automatico", "critico", false,
                "Primeiro contato", "Lead priorizado para abordagem comercial imediata"),
            createLead("PR-ALP", "BELL 429", "HELICÓPTERO", "2026-08-10", "2026-08-28", "22334455000166", "Alpina Táxi Aéreo", "21997766123", "v1", "renata", "atencao", true,
                "Orçamento enviado", "Proposta enviada para diretoria técnica"),
            createLead("PS-OCE", "S-76C", "HELICÓPTERO", "2026-10-02", "2026-10-19", "33445566000177", "Ocean Air", "41990009988", "v2", "automatico", "normal", false,
                "Ligar novamente", "Cliente pediu contato após fechamento do trimestre"),
            createLead("PP-SKY", "PA-46 MALIBU", "TURBOÉLICE", "2026-06-30", "2026-07-22", "44556677000188", "Sky Services", "31981122334", null, "automatico", "critico", false,
                "Primeiro contato", "Contato inicial feito com financeiro da empresa"),
            createLead("PR-NAV", "CITATION XLS+", "JATO LEVE", "2026-11-15", "2026-11-29", "55667788000199", "Navega Aero", "11987654321", "v1", "renata", "normal", false,
                "Sem sucesso", "Sem retorno após envio de apresentação institucional"));

        List<Lead> savedSeeds = leadRepository.saveAll(seeds);

        // Publica comandos de resolução de cliente via RabbitMQ (assíncrono)
        savedSeeds.forEach(lead -> {
            LeadClient client = lead.getClient();
            leadEventPublisher.publishClientResolveCommand(
                    new LeadClientResolveCommand(lead.getId(), client.getCnpj(), client.getName(), client.getPhone()));
        });
    }

    private Lead createLead(String prefix, String model, String typeLabel, String tboDate, String cvaDate, String cnpj, String clientName, String phone, String assignedTo, String assignedBy, String priority, boolean dailyCompleted, String historyStatus, String historyNote) {
        Lead lead = new Lead();
        lead.setPrefix(prefix);
        lead.setModel(model);
        lead.setTypeLabel(typeLabel);
        lead.setTboDate(tboDate);
        lead.setCvaDate(cvaDate);
        lead.setClient(new LeadClient(cnpj, clientName, phone));
        lead.setAssignedTo(assignedTo);
        lead.setAssignedBy(assignedBy);
        lead.setPriority(priority);
        lead.setDailyCompleted(dailyCompleted);
        lead.addHistoryEntry(historyStatus, historyNote);
        return lead;
    }
}