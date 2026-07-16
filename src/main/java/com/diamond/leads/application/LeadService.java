package com.diamond.leads.application;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.diamond.leads.application.dto.LeadCreateRequest;
import com.diamond.leads.application.dto.LeadAssignmentRequest;
import com.diamond.leads.application.dto.LeadHistoryRequest;
import com.diamond.leads.application.dto.LeadResponse;
import com.diamond.leads.application.dto.LeadUpdateRequest;
import com.diamond.leads.domain.LeadClient;
import com.diamond.leads.domain.Lead;
import com.diamond.leads.domain.LeadStatus;
import com.diamond.leads.infrastructure.persistence.repository.LeadRepository;
import com.diamond.leads.infrastructure.persistence.repository.LeadHistoryRepository;

@Service
@Transactional
public class LeadService implements ILeadService {

    private static final DateTimeFormatter HISTORY_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final LeadRepository leadRepository;
    private final LeadHistoryRepository leadHistoryRepository;

    public LeadService(LeadRepository leadRepository, LeadHistoryRepository leadHistoryRepository) {
        this.leadRepository = leadRepository;
        this.leadHistoryRepository = leadHistoryRepository;
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

        return LeadResponse.fromEntity(leadRepository.save(lead));
    }

    @Override
    public LeadResponse assignLead(UUID id, LeadAssignmentRequest request) {
        Lead lead = getLeadOrThrow(id);
        lead.setAssignedTo(normalizeOptionalText(request.assignedTo()));
        lead.setAssignedBy(normalizeOptionalText(request.assignedBy()) == null ? "renata" : request.assignedBy().trim());
        lead.setDailyCompleted(false);
        return LeadResponse.fromEntity(leadRepository.save(lead));
    }

    @Override
    public LeadResponse addHistory(UUID id, LeadHistoryRequest request) {
        Lead lead = getLeadOrThrow(id);
        lead.addHistoryEntry(request.status(), request.note().trim());
        lead.setDailyCompleted(true);
        lead.setStatus(mapStatus(request.status()));
        return LeadResponse.fromEntity(leadRepository.save(lead));
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