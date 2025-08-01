#!/bin/bash

# =============================================================================
# OpenTelemetry Inspector - Application Launch Script
# =============================================================================
# This script provides two modes for running the Spring Boot application:
# 1. Normal mode: Direct connection to Azure Application Insights
# 2. Proxy mode: Routes telemetry through local proxy for inspection
# =============================================================================

export APPLICATIONINSIGHTS_CONNECTION_STRING="[FILL OUT]"

# Check if proxy mode is requested
if [ "$1" = "--proxy" ]; then
    echo "🚀 Starting application in PROXY mode (telemetry will be routed through local proxy)"
    
    # Start Spring Boot application with proxy configuration
    # The proxy settings route all HTTP/HTTPS traffic through localhost:8888
    mvn spring-boot:run \
      -Dspring-boot.run.jvmArguments="\
      -javaagent:src/applicationinsights-agent-3.7.1.jar \
      -Dhttp.proxyHost=localhost \
      -Dhttp.proxyPort=8888 \
      -Dhttps.proxyHost=localhost \
      -Dhttps.proxyPort=8888"

else
    echo "🚀 Starting application in NORMAL mode (direct connection to Azure Application Insights)"
    
    # Start Spring Boot application with Application Insights agent
    # The agent automatically collects telemetry and sends it to Azure
    mvn spring-boot:run \
      -Dspring-boot.run.jvmArguments="\
      -javaagent:src/applicationinsights-agent-3.7.1.jar"

fi