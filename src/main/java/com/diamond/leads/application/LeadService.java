package com.diamond.leads.application;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.diamond.leads.application.dto.LeadCreateRequest;
import com.diamond.leads.application.dto.LeadAssignmentRequest;
import com.diamond.leads.application.dto.LeadClientDetailsResponse;
import com.diamond.leads.application.dto.LeadHistoryRequest;
import com.diamond.leads.application.dto.LeadResponse;
import com.diamond.leads.application.dto.LeadUpdateRequest;
import com.diamond.leads.domain.LeadClient;
import com.diamond.leads.domain.Lead;
import com.diamond.leads.domain.LeadStatus;
import com.diamond.leads.infrastructure.client.ClientIntegrationService;
import com.diamond.leads.infrastructure.messaging.LeadEventPublisher;
import com.diamond.leads.infrastructure.messaging.event.LeadAssignedEvent;
import com.diamond.leads.infrastructure.messaging.event.LeadClientResolveCommand;
import com.diamond.leads.infrastructure.messaging.event.LeadStatusChangedEvent;
import com.diamond.leads.infrastructure.persistence.repository.LeadRepository;
import com.diamond.leads.infrastructure.persistence.repository.LeadHistoryRepository;

@Service
@Transactional
public class LeadService implements ILeadService {

    private static final DateTimeFormatter HISTORY_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final LeadRepository leadRepository;
    private final LeadHistoryRepository leadHistoryRepository;
    private final ClientIntegrationService clientIntegrationService;
    private final LeadEventPublisher leadEventPublisher;

    public LeadService(LeadRepository leadRepository, LeadHistoryRepository leadHistoryRepository,
            ClientIntegrationService clientIntegrationService, LeadEventPublisher leadEventPublisher) {
        this.leadRepository = leadRepository;
        this.leadHistoryRepository = leadHistoryRepository;
        this.clientIntegrationService = clientIntegrationService;
        this.leadEventPublisher = leadEventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeadResponse> findAll() {
        return leadRepository.findAll().stream()
                .map(LeadResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LeadResponse findById(UUID id) {
        Lead lead = getLeadOrThrow(id);
        return LeadResponse.fromEntity(lead);
    }

    @Override
    public LeadResponse create(LeadCreateRequest request) {
        Lead lead = new Lead();
        lead.setPrefix(request.prefix());
        lead.setModel(request.model());
        lead.setTypeLabel(request.typeLabel());
        lead.setTboDate(request.tboDate());
        lead.setCvaDate(request.cvaDate());
        lead.setClient(new LeadClient(request.client().cnpj(), request.client().name(), request.client().phone()));
        lead.setAssignedTo(normalizeOptionalText(request.assignedTo()));
        lead.setAssignedBy("renata");
        lead.setPriority(getPriorityByDates(request.tboDate(), request.cvaDate()));
        lead.setDailyCompleted(false);
        lead.setStatus(LeadStatus.NOVO);

        Lead savedLead = leadRepository.save(lead);

        // Command Message: pede ao client-service que resolva o cliente (assíncrono via RabbitMQ)
        leadEventPublisher.publishClientResolveCommand(
                new LeadClientResolveCommand(
                        savedLead.getId(),
                        request.client().cnpj(),
                        request.client().name(),
                        request.client().phone()));

        return LeadResponse.fromEntity(savedLead);
    }

    @Override
    public LeadResponse update(UUID id, LeadUpdateRequest request) {
        Lead lead = getLeadOrThrow(id);
        lead.setPrefix(request.prefix());
        lead.setModel(request.model());
        lead.setTypeLabel(request.typeLabel());
        lead.setTboDate(request.tboDate());
        lead.setCvaDate(request.cvaDate());
        lead.setClient(new LeadClient(request.client().cnpj(), request.client().name(), request.client().phone()));
        lead.setAssignedTo(normalizeOptionalText(request.assignedTo()));
        lead.setPriority(getPriorityByDates(request.tboDate(), request.cvaDate()));
        lead.setDailyCompleted(Boolean.TRUE.equals(request.dailyCompleted()));

        Lead saved = leadRepository.save(lead);

        // Command Message: resolve cliente de forma assíncrona via RabbitMQ
        leadEventPublisher.publishClientResolveCommand(
                new LeadClientResolveCommand(
                        saved.getId(),
                        request.client().cnpj(),
                        request.client().name(),
                        request.client().phone()));

        return LeadResponse.fromEntity(saved);
    }

    @Override
    public LeadResponse assignLead(UUID id, LeadAssignmentRequest request) {
        Lead lead = getLeadOrThrow(id);
        lead.setAssignedTo(normalizeOptionalText(request.assignedTo()));
        lead.setAssignedBy(normalizeOptionalText(request.assignedBy()) == null ? "renata" : request.assignedBy().trim());
        lead.setDailyCompleted(false);
        Lead saved = leadRepository.save(lead);

        // Event Notification: notifica atribuição de lead
        leadEventPublisher.publishLeadAssigned(
                new LeadAssignedEvent(
                        saved.getId(),
                        saved.getPrefix(),
                        saved.getAssignedTo(),
                        saved.getAssignedBy(),
                        LocalDateTime.now().toString()));

        return LeadResponse.fromEntity(saved);
    }

    @Override
    public LeadResponse addHistory(UUID id, LeadHistoryRequest request) {
        Lead lead = getLeadOrThrow(id);
        String oldStatus = lead.getStatus().name();
        lead.addHistoryEntry(request.status(), request.note().trim());
        lead.setDailyCompleted(true);
        lead.setStatus(mapStatus(request.status()));
        Lead saved = leadRepository.save(lead);

        // Event Notification: notifica mudança de status
        leadEventPublisher.publishStatusChanged(
                new LeadStatusChangedEvent(
                        saved.getId(),
                        saved.getPrefix(),
                        oldStatus,
                        saved.getStatus().name(),
                        LocalDateTime.now().toString()));

        return LeadResponse.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeadResponse.HistoryResponse> findHistoryByLeadId(UUID id) {
        getLeadOrThrow(id);
        return leadHistoryRepository.findByLeadIdOrderByTimestampDesc(id).stream()
                .map(entry -> new LeadResponse.HistoryResponse(
                        entry.getId(),
                        entry.getStatus(),
                        entry.getNote(),
                        entry.getTimestamp().format(HISTORY_TIMESTAMP_FORMAT)))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LeadClientDetailsResponse findClientDetails(UUID id) {
        Lead lead = getLeadOrThrow(id);
        LeadClient cached = lead.getClient();

        if (lead.getClientId() == null) {
            return LeadClientDetailsResponse.cachedOnly(cached, "SEM_CLIENT_ID");
        }

        // Query síncrona via Feign — mantida para consultas que precisam de resposta imediata
        return clientIntegrationService.findClientById(lead.getClientId())
                .map(dto -> LeadClientDetailsResponse.enriched(cached, dto))
                .orElseGet(() -> LeadClientDetailsResponse.cachedOnly(cached, "CLIENT_SERVICE_INDISPONIVEL"));
    }

    @Override
    public void delete(UUID id) {
        Lead lead = getLeadOrThrow(id);
        leadRepository.delete(lead);
    }

    private Lead getLeadOrThrow(UUID id) {
        return leadRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead não encontrado"));
    }

    private String getPriorityByDates(String tboDate, String cvaDate) {
        long nearestDays = Math.min(daysUntil(tboDate), daysUntil(cvaDate));
        if (nearestDays <= 7) {
            return "critico";
        }
        if (nearestDays <= 30) {
            return "atencao";
        }
        return "normal";
    }

    private long daysUntil(String date) {
        java.time.LocalDate target = java.time.LocalDate.parse(date);
        java.time.LocalDate today = java.time.LocalDate.now();
        return java.time.temporal.ChronoUnit.DAYS.between(today, target);
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private LeadStatus mapStatus(String status) {
        if ("Fechado".equalsIgnoreCase(status)) {
            return LeadStatus.FECHADO;
        }

        return LeadStatus.ATENDIMENTO;
    }
}