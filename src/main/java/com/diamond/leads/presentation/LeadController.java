package com.diamond.leads.presentation;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.diamond.leads.application.ILeadService;
import com.diamond.leads.application.dto.LeadAssignmentRequest;
import com.diamond.leads.application.dto.LeadCreateRequest;
import com.diamond.leads.application.dto.LeadHistoryRequest;
import com.diamond.leads.application.dto.LeadResponse;
import com.diamond.leads.application.dto.LeadUpdateRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/leads")
@CrossOrigin(origins = "http://localhost:5173")
@Validated
public class LeadController {

    private final ILeadService leadService;

    public LeadController(ILeadService leadService) {
        this.leadService = leadService;
    }

    @GetMapping
    public List<LeadResponse> list() {
        return leadService.findAll();
    }

    @GetMapping("/{id}")
    public LeadResponse getById(@PathVariable UUID id) {
        return leadService.findById(id);
    }

    @PostMapping
    public ResponseEntity<LeadResponse> create(@Valid @RequestBody LeadCreateRequest request) {
        LeadResponse createdLead = leadService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdLead.id())
                .toUri();

        return ResponseEntity.created(location).body(createdLead);
    }

    @PutMapping("/{id}")
    public LeadResponse update(@PathVariable UUID id, @Valid @RequestBody LeadUpdateRequest request) {
        return leadService.update(id, request);
    }

    @PostMapping("/{id}/assignment")
    public LeadResponse assignLead(@PathVariable UUID id, @Valid @RequestBody LeadAssignmentRequest request) {
        return leadService.assignLead(id, request);
    }

    @PostMapping("/{id}/history")
    public LeadResponse addHistory(@PathVariable UUID id, @Valid @RequestBody LeadHistoryRequest request) {
        return leadService.addHistory(id, request);
    }

    @GetMapping("/{id}/history")
    public List<LeadResponse.HistoryResponse> getHistory(@PathVariable UUID id) {
        return leadService.findHistoryByLeadId(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        leadService.delete(id);
        return ResponseEntity.noContent().build();
    }
}