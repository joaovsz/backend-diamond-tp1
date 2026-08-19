package com.diamond.clients.presentation;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import com.diamond.clients.application.IClientService;
import com.diamond.clients.application.dto.ClientResponse;

@WebMvcTest(ClientController.class)
class ClientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IClientService clientService;

    @Test
    void getById_returnsNotFound_whenClientDoesNotExist() throws Exception {
        UUID id = UUID.randomUUID();
        when(clientService.findById(id))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado"));

        mockMvc.perform(get("/api/clients/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_returnsBadRequest_whenCnpjBlank() throws Exception {
        mockMvc.perform(post("/api/clients")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cnpj\":\"\",\"name\":\"Cliente\",\"phone\":\"11999990000\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void findOrCreate_returnsClient() throws Exception {
        UUID id = UUID.randomUUID();
        when(clientService.findOrCreate(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new ClientResponse(id, "00000000000000", "Cliente Teste", "11999990000",
                        null, null, null, "10/07/2026 09:00"));

        mockMvc.perform(post("/api/clients/find-or-create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cnpj\":\"00000000000000\",\"name\":\"Cliente Teste\",\"phone\":\"11999990000\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cnpj").value("00000000000000"))
                .andExpect(jsonPath("$.name").value("Cliente Teste"));
    }
}
