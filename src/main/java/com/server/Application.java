package com.server;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

@SpringBootApplication
@RestController
public class Application {
    private static final Tracer tracer = GlobalOpenTelemetry.getTracer("java-ot-inspector");
    private final RestTemplate restTemplate;

    public Application(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @GetMapping("/")
    public String hello() {
        // Add tracestate to current span
        Span.current().setAttribute("author", "john-doe");
        Span.current().setAttribute("env", "dev");
        
        // Create tracestate that will be propagated
        TraceState tracestate = TraceState.builder()
            .put("tenant", "acme")
            .put("user", "john-doe")
            .build();
        
        // Set tracestate on current span context
        Span.current().setAttribute("tracestate.tenant", "acme");
        Span.current().setAttribute("tracestate.user", "john-doe");
        
        // This HTTP call should now auto-propagate tracestate headers
        ResponseEntity<String> healthResponse = restTemplate.getForEntity("http://localhost:8080/health", String.class);
        
        return "Hello, World!\nHealth: " + healthResponse.getBody();
    }

    @GetMapping("/health")
    public String health() {
        // Add tracestate to health span
        Span.current().setAttribute("service.type", "health-checker");
        Span.current().setAttribute("tracestate.service", "health-checker");
        
        return "Application healthy!";
    }

    @Configuration
    public static class RestTemplateConfig {

        @Bean
        public RestTemplate restTemplate() {
            return new RestTemplate();
        }
    }

    @Configuration
    public static class OpenTelemetryConfig {

        @Bean
        public OpenTelemetry openTelemetry() {
            SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(OtlpGrpcSpanExporter.builder().build()))
                .setResource(Resource.getDefault())
                .build();

            OpenTelemetrySdk openTelemetrySdk = OpenTelemetrySdk.builder()
                .setTracerProvider(sdkTracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .build();

            return openTelemetrySdk;
        }
    }
}
