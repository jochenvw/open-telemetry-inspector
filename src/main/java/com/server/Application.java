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
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.api.trace.SpanContext;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

@SpringBootApplication
@RestController
public class Application {
    private final RestTemplate restTemplate;
    private final Tracer tracer;

    public Application(RestTemplate restTemplate, OpenTelemetry openTelemetry) {
        this.restTemplate = restTemplate;
        this.tracer = openTelemetry.getTracer("java-ot-inspector");
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @GetMapping("/")
    public String hello() {
        // Start a new span to ensure we have a valid trace context
        Span span = tracer.spanBuilder("hello-operation").startSpan();
        
        try (io.opentelemetry.context.Scope outerScope = span.makeCurrent()) {
            // Add attributes to current span
            span.setAttribute("author", "john-doe");
            span.setAttribute("env", "dev");
            
            // Create tracestate that will be propagated
            TraceState tracestate = TraceState.builder()
                .put("tenant", "acme")
                .put("user", "john-doe")
                .build();
            
            // Get current span and create new span context with tracestate
            SpanContext currentSpanContext = span.getSpanContext();
            SpanContext newSpanContext = SpanContext.create(
                currentSpanContext.getTraceId(),
                currentSpanContext.getSpanId(),
                currentSpanContext.getTraceFlags(),
                tracestate
            );
            
            // Update the current context with the new span context
            Context newContext = Context.current().with(Span.wrap(newSpanContext));
            
            // Execute the HTTP call within the updated context
            try (io.opentelemetry.context.Scope scope = newContext.makeCurrent()) {
                ResponseEntity<String> healthResponse = restTemplate.getForEntity("http://localtest.me:8080/health", String.class);
                return "Hello, World!\nHealth: " + healthResponse.getBody();
            } catch (Exception e) {
                return "Hello, World!\nHealth check failed: " + e.getMessage();
            }
        } finally {
            span.end();
        }
    }

    @GetMapping("/health")
    public String health() {
        // Add attributes to health span
        Span.current().setAttribute("service.type", "health-checker");
        
        return "Application healthy!";
    }

    @Configuration
    public static class RestTemplateConfig {

        @Bean
        public RestTemplate restTemplate(OpenTelemetry openTelemetry) {
            RestTemplate restTemplate = new RestTemplate();
            
            // Add interceptor to propagate OpenTelemetry context
            List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
            interceptors.add(new OpenTelemetryInterceptor(openTelemetry));
            restTemplate.setInterceptors(interceptors);
            
            return restTemplate;
        }
    }
    
    // Custom interceptor to propagate OpenTelemetry context headers
    public static class OpenTelemetryInterceptor implements ClientHttpRequestInterceptor {
        private final TextMapPropagator propagator;
        
        public OpenTelemetryInterceptor(OpenTelemetry openTelemetry) {
            this.propagator = openTelemetry.getPropagators().getTextMapPropagator();
        }
        
        @Override
        public ClientHttpResponse intercept(
            HttpRequest request, 
            byte[] body, 
            ClientHttpRequestExecution execution) throws IOException {
            
            // Inject the current context (including tracestate) into HTTP headers
            propagator.inject(Context.current(), request, (carrier, key, value) -> {
                carrier.getHeaders().add(key, value);
            });
            
            return execution.execute(request, body);
        }
    }

    @Configuration
    public static class OpenTelemetryConfig {

        @Bean
        public OpenTelemetry openTelemetry() {
            SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(SimpleSpanProcessor.create(
                    OtlpGrpcSpanExporter.builder()
                        .setEndpoint("https://westeurope-5.in.applicationinsights.azure.com/")
                        .addHeader("Authorization", "Bearer " + System.getenv("APPLICATIONINSIGHTS_CONNECTION_STRING"))
                        .build()
                ))
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
