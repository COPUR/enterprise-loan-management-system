#!/bin/bash
# Quick start script for local development

set -e

echo "🏦 Starting Banking System Development Environment..."

# Load environment variables
if [ -f .env.local ]; then
    export "$(cat .env.local | grep -v '^#' | xargs)"
fi

# Create directories
mkdir -p logs/dev data/dev

# Start with hot reload
echo "Starting application with hot reload..."
./gradlew bootRun

echo "✅ Development server started!"
echo "🌐 Application: http://localhost:8080"
echo "🔍 H2 Console: http://localhost:8080/h2-console"
echo "📚 Swagger UI: http://localhost:8080/swagger-ui.html"
echo "📊 Actuator: http://localhost:8080/actuator"
