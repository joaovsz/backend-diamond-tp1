package com.diamond.leads.presentation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.diamond.leads.infrastructure.messaging.LeadEventPublisher;
import com.diamond.leads.infrastructure.client.ClientIntegrationService;

import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability
class ActuatorHealthEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RabbitTemplate rabbitTemplate;

    @MockBean
    private ConnectionFactory connectionFactory;

    @MockBean
    private LeadEventPublisher leadEventPublisher;

    @MockBean
    private ClientIntegrationService clientIntegrationService;

    @Test
    @DisplayName("Deve responder 200 no endpoint geral de health do Actuator")
    void healthEndpointReturnsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Deve responder 200 na probe de liveness para o Kubernetes")
    void livenessProbeReturnsUp() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Deve responder 200 na probe de readiness para o Kubernetes")
    void readinessProbeReturnsUp() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("Deve expor métricas do Prometheus para monitoramento")
    void prometheusEndpointReturnsOk() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk());
    }
}
