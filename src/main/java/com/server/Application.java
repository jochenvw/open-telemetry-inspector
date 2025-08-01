package com.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import io.opentelemetry.instrumentation.annotations.WithSpan;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.GlobalOpenTelemetry;

@SpringBootApplication
@RestController
public class Application {
    private static final Tracer tracer = GlobalOpenTelemetry.getTracer("my-app");

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    
    @GetMapping("/")
    @WithSpan("landing-page_wrapper")
    public String hello() {
        Span span = tracer.spanBuilder("landing-page").startSpan();
        try {
            span.addEvent("Getting landing page ...");
        } finally {
            span.end();
        }
        return "Hello, World!";
    }

    @GetMapping("/health")
    @WithSpan("healthcheck_wrapper")
    public String health() {
        Span span = tracer.spanBuilder("healthcheck").startSpan();
        try {
            span.addEvent("Getting health status ...");
        } finally {
            span.end();
        }

        return "OK";
    }
}
