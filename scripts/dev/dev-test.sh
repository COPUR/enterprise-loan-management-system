#!/bin/bash
# Quick test script for development

set -e

echo "🧪 Running Development Tests..."

# Load environment variables
if [ -f .env.local ]; then
    export $(cat .env.local | grep -v '^#' | xargs)
fi

# Run tests with development profile
./gradlew testDev --continue

echo "✅ Development tests completed!"
