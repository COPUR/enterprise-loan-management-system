
#!/bin/bash

# =======================================================================
# GraalVM Setup Script for Enterprise Loan Management System
# =======================================================================

set -euo pipefail

# Configuration
GRAALVM_VERSION="21.0.2"
GRAALVM_JAVA_VERSION="21"
INSTALL_DIR="$HOME/.graalvm"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    # Check if running on supported platform
    case "$(uname -s)" in
        Linux*)     MACHINE=linux;;
        Darwin*)    MACHINE=macos;;
        *)          log_error "Unsupported platform: $(uname -s)"; exit 1;;
    esac
    
    # Check architecture
    case "$(uname -m)" in
        x86_64)     ARCH=amd64;;
        aarch64)    ARCH=aarch64;;
        arm64)      ARCH=aarch64;;
        *)          log_error "Unsupported architecture: $(uname -m)"; exit 1;;
    esac
    
    log_success "Platform: $MACHINE, Architecture: $ARCH"
    
    # Check required tools
    command -v curl >/dev/null 2>&1 || { log_error "curl is required but not installed."; exit 1; }
    command -v tar >/dev/null 2>&1 || { log_error "tar is required but not installed."; exit 1; }
    
    log_success "Prerequisites check completed"
}

# Download and install GraalVM
install_graalvm() {
    log_info "Installing GraalVM $GRAALVM_VERSION..."
    
    # Create installation directory
    mkdir -p "$INSTALL_DIR"
    
    # Determine download URL based on platform
    if [ "$MACHINE" = "linux" ]; then
        DOWNLOAD_URL="https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-${GRAALVM_VERSION}/graalvm-community-jdk-${GRAALVM_VERSION}_${MACHINE}-${ARCH}_bin.tar.gz"
    elif [ "$MACHINE" = "macos" ]; then
        DOWNLOAD_URL="https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-${GRAALVM_VERSION}/graalvm-community-jdk-${GRAALVM_VERSION}_${MACHINE}-${ARCH}_bin.tar.gz"
    fi
    
    DOWNLOAD_FILE="$INSTALL_DIR/graalvm-${GRAALVM_VERSION}.tar.gz"
    
    # Download GraalVM
    log_info "Downloading GraalVM from: $DOWNLOAD_URL"
    curl -L -o "$DOWNLOAD_FILE" "$DOWNLOAD_URL"
    
    # Extract GraalVM
    log_info "Extracting GraalVM..."
    cd "$INSTALL_DIR"
    tar -xzf "graalvm-${GRAALVM_VERSION}.tar.gz"
    
    # Find the extracted directory
    GRAALVM_HOME=$(find "$INSTALL_DIR" -maxdepth 1 -type d -name "graalvm-*" | head -1)
    
    if [ -z "$GRAALVM_HOME" ]; then
        log_error "Failed to find extracted GraalVM directory"
        exit 1
    fi
    
    # Clean up download file
    rm -f "$DOWNLOAD_FILE"
    
    log_success "GraalVM installed to: $GRAALVM_HOME"
    echo "$GRAALVM_HOME" > "$INSTALL_DIR/current"
}

# Install Native Image
install_native_image() {
    local graalvm_home="$1"
    
    log_info "Installing Native Image component..."
    
    if [ "$MACHINE" = "macos" ]; then
        "$graalvm_home/Contents/Home/bin/gu" install native-image
    else
        "$graalvm_home/bin/gu" install native-image
    fi
    
    log_success "Native Image component installed"
}

# Configure environment
configure_environment() {
    local graalvm_home="$1"
    
    log_info "Configuring environment..."
    
    # Create environment configuration file
    cat > "$INSTALL_DIR/graalvm-env.sh" << EOF
#!/bin/bash
# GraalVM Environment Configuration for Enterprise Loan Management System

export GRAALVM_HOME="$graalvm_home"
export JAVA_HOME="\$GRAALVM_HOME"
export PATH="\$GRAALVM_HOME/bin:\$PATH"

# Native Image specific settings
export NATIVE_IMAGE_CONFIG_OUTPUT_DIR="\$PWD/native-image-config"

# Performance tuning for native compilation
export NATIVE_IMAGE_OPTS="-H:+UnlockExperimentalVMOptions -H:+UseContainerSupport -H:+UseG1GC"

# Banking system specific configurations
export BANKING_NATIVE_MODE=true
export SPRING_AOT_ENABLED=true

echo "GraalVM environment configured"
echo "Java Home: \$JAVA_HOME"
echo "GraalVM Version: \$(java -version 2>&1 | head -n 1)"
echo "Native Image: \$(which native-image)"
EOF
    
    chmod +x "$INSTALL_DIR/graalvm-env.sh"
    
    # Add to shell profile if requested
    read -p "Add GraalVM to your shell profile? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "source $INSTALL_DIR/graalvm-env.sh" >> ~/.bashrc
        echo "source $INSTALL_DIR/graalvm-env.sh" >> ~/.zshrc 2>/dev/null || true
        log_success "GraalVM environment added to shell profile"
    fi
    
    log_success "Environment configuration completed"
}

# Verify installation
verify_installation() {
    local graalvm_home="$1"
    
    log_info "Verifying GraalVM installation..."
    
    # Source environment
    source "$INSTALL_DIR/graalvm-env.sh"
    
    # Test Java
    java -version
    
    # Test Native Image
    if command -v native-image >/dev/null 2>&1; then
        native-image --version
        log_success "Native Image is available"
    else
        log_error "Native Image is not available"
        exit 1
    fi
    
    # Test Gradle compatibility
    cd "$PROJECT_ROOT"
    if [ -f "./gradlew" ]; then
        ./gradlew --version
        log_success "Gradle compatibility verified"
    fi
    
    log_success "GraalVM installation verification completed"
}

# Main installation process
main() {
    log_info "Starting GraalVM setup for Enterprise Loan Management System"
    
    check_prerequisites
    
    # Check if already installed
    if [ -f "$INSTALL_DIR/current" ]; then
        EXISTING_GRAALVM_HOME=$(cat "$INSTALL_DIR/current")
        if [ -d "$EXISTING_GRAALVM_HOME" ]; then
            log_warning "GraalVM already installed at: $EXISTING_GRAALVM_HOME"
            read -p "Reinstall? (y/n): " -n 1 -r
            echo
            if [[ ! $REPLY =~ ^[Yy]$ ]]; then
                log_info "Using existing installation"
                configure_environment "$EXISTING_GRAALVM_HOME"
                verify_installation "$EXISTING_GRAALVM_HOME"
                return 0
            fi
        fi
    fi
    
    install_graalvm
    
    GRAALVM_HOME=$(cat "$INSTALL_DIR/current")
    install_native_image "$GRAALVM_HOME"
    configure_environment "$GRAALVM_HOME"
    verify_installation "$GRAALVM_HOME"
    
    log_success "GraalVM setup completed successfully!"
    log_info "To use GraalVM in current session, run: source $INSTALL_DIR/graalvm-env.sh"
    log_info "To build native image, run: $PROJECT_ROOT/scripts/graalvm-build.sh"
}

# Run main function
main "$@"
