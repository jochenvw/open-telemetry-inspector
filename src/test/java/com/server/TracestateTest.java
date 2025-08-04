package com.server;

import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "logging.level.root=INFO"
})
public class TracestateTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void testTracestateCreation() {
        // Test that TraceState can be created with the expected values
        TraceState tracestate = TraceState.builder()
            .put("tenant", "acme")
            .put("user", "john-doe")
            .build();
        
        assertThat(tracestate.get("tenant")).isEqualTo("acme");
        assertThat(tracestate.get("user")).isEqualTo("john-doe");
    }

    @Test
    public void testApplicationStartsSuccessfully() {
        // Test that the application starts and responds to requests
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/health", 
            String.class
        );
        
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("Application healthy!");
    }

    @Test
    public void testHelloEndpointHandlesErrors() {
        // Test that the hello endpoint gracefully handles connection errors
        // (since localtest.me won't resolve in test environment)
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/", 
            String.class
        );
        
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains("Hello, World!");
        // Should contain error message since localtest.me won't resolve
        assertThat(response.getBody()).contains("Health check failed:");
    }
}