package com.diamond.discovery;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DiscoveryServerApplicationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int port;

    @Test
    @DisplayName("Contexto do Discovery Server deve subir com sucesso")
    void contextLoads() {
        assertThat(port).isGreaterThan(0);
    }

    @Test
    @DisplayName("Deve responder 200 no endpoint de health do Actuator no Discovery Server")
    void healthEndpointReturnsUp() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    @DisplayName("Deve responder 200 na probe de liveness para o Kubernetes no Discovery Server")
    void livenessProbeReturnsUp() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health/liveness", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }

    @Test
    @DisplayName("Deve responder 200 na probe de readiness para o Kubernetes no Discovery Server")
    void readinessProbeReturnsUp() {
        ResponseEntity<String> response = restTemplate.getForEntity("/actuator/health/readiness", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"status\":\"UP\"");
    }
}
