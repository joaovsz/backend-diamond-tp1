package com.diamond.leads.presentation;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.diamond.leads.application.ILeadService;
import com.diamond.leads.application.dto.LeadClientDetailsResponse;
import com.diamond.leads.application.dto.LeadResponse;

@WebMvcTest(LeadController.class)
class LeadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ILeadService leadService;

    @Test
    void getHistory_returnsHistoryList() throws Exception {
        UUID leadId = UUID.randomUUID();
        UUID historyId = UUID.randomUUID();
        when(leadService.findHistoryByLeadId(leadId)).thenReturn(List.of(
                new LeadResponse.HistoryResponse(historyId, "Primeiro contato", "Nota de teste", "10/07/2026 09:00")));

        mockMvc.perform(get("/api/leads/{id}/history", leadId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("Primeiro contato"))
                .andExpect(jsonPath("$[0].note").value("Nota de teste"));
    }

    @Test
    void getHistory_returnsNotFound_whenLeadDoesNotExist() throws Exception {
        UUID leadId = UUID.randomUUID();
        when(leadService.findHistoryByLeadId(leadId))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Lead não encontrado"));

        mockMvc.perform(get("/api/leads/{id}/history", leadId))
                .andExpect(status().isNotFound());
    }

    @Test
    void addHistory_returnsBadRequest_whenNoteIsBlank() throws Exception {
        UUID leadId = UUID.randomUUID();

        mockMvc.perform(post("/api/leads/{id}/history", leadId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"Fechado\",\"note\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getClient_returnsEnrichedDetails() throws Exception {
        UUID leadId = UUID.randomUUID();
        when(leadService.findClientDetails(leadId)).thenReturn(new LeadClientDetailsResponse(
                "00000000000000", "Cliente Teste", "11999990000",
                "Executivo", 5, "Cliente estratégico", true, null));

        mockMvc.perform(get("/api/leads/{id}/client", leadId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enriched").value(true))
                .andExpect(jsonPath("$.unavailableReason").doesNotExist())
                .andExpect(jsonPath("$.marketSegment").value("Executivo"))
                .andExpect(jsonPath("$.fleetSize").value(5));
    }

    @Test
    void getClient_returnsCachedOnlyWhenServiceUnavailable() throws Exception {
        UUID leadId = UUID.randomUUID();
        when(leadService.findClientDetails(leadId)).thenReturn(new LeadClientDetailsResponse(
                "00000000000000", "Cliente Teste", "11999990000",
                null, null, null, false, "CLIENT_SERVICE_INDISPONIVEL"));

        mockMvc.perform(get("/api/leads/{id}/client", leadId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enriched").value(false))
                .andExpect(jsonPath("$.unavailableReason").value("CLIENT_SERVICE_INDISPONIVEL"))
                .andExpect(jsonPath("$.marketSegment").doesNotExist());
    }
}
