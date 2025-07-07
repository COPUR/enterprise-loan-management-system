#!/bin/bash
export JAVA_HOME="/nix/store/$(ls /nix/store | grep -E 'openjdk.*21' | head -1)"
export PATH="$JAVA_HOME/bin:$PATH"

echo "Starting Enterprise Loan Management System with Java 21"
echo "Using Java: $(java -version 2>&1 | head -1)"

# Compile all Java files
mkdir -p build/classes
find src -name "*.java" -exec javac -d build/classes {} +

# Run the standalone application
java -cp build/classes com.bank.loanmanagement.StandaloneApplication