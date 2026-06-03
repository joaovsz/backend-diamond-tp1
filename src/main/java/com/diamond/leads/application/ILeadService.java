package com.diamond.leads.application;

import java.util.List;
import java.util.UUID;

import com.diamond.leads.application.dto.LeadCreateRequest;
import com.diamond.leads.application.dto.LeadAssignmentRequest;
import com.diamond.leads.application.dto.LeadHistoryRequest;
import com.diamond.leads.application.dto.LeadResponse;
import com.diamond.leads.application.dto.LeadUpdateRequest;

public interface ILeadService {

    List<LeadResponse> findAll();

    LeadResponse findById(UUID id);

    LeadResponse create(LeadCreateRequest request);

    LeadResponse update(UUID id, LeadUpdateRequest request);

    LeadResponse assignLead(UUID id, LeadAssignmentRequest request);

    LeadResponse addHistory(UUID id, LeadHistoryRequest request);

    void delete(UUID id);
}