#!/bin/bash
# Reset development database

set -e

echo "🗄️ Resetting Development Database..."

# Stop application if running
pkill -f "spring-boot:run" || true

# Clean development files
./gradlew cleanDev

# Load fresh test data
./gradlew loadTestData

echo "✅ Development database reset complete!"
