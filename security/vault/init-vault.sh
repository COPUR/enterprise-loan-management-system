#!/bin/bash

# Enterprise Banking Vault Initialization
# Production-grade secrets management setup

set -e

VAULT_ADDR="https://vault:8200"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[$(date +'%Y-%m-%d %H:%M:%S')] ✅ $1${NC}"
}

log_error() {
    echo -e "${RED}[$(date +'%Y-%m-%d %H:%M:%S')] ❌ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}[$(date +'%Y-%m-%d %H:%M:%S')] ⚠️  $1${NC}"
}

# Wait for Vault to be ready
wait_for_vault() {
    log "🔒 Waiting for Vault to be ready..."
    local timeout=60
    local count=0
    
    while [ $count -lt $timeout ]; do
        if curl -k -s "$VAULT_ADDR/v1/sys/health" >/dev/null 2>&1; then
            log_success "Vault is ready"
            return 0
        fi
        count=$((count + 1))
        sleep 1
    done
    
    log_error "Vault failed to start within $timeout seconds"
    return 1
}

# Initialize Vault
initialize_vault() {
    log "🔐 Initializing Vault..."
    
    # Check if already initialized
    if vault status >/dev/null 2>&1; then
        log_warning "Vault is already initialized"
        return 0
    fi
    
    # Initialize with 5 key shares, 3 required for unseal
    local init_output=$(vault operator init \
        -key-shares=5 \
        -key-threshold=3 \
        -format=json)
    
    # Save unseal keys and root token securely
    echo "$init_output" | jq -r '.unseal_keys_b64[]' > "$SCRIPT_DIR/vault-unseal-keys.txt"
    echo "$init_output" | jq -r '.root_token' > "$SCRIPT_DIR/vault-root-token.txt"
    
    # Set restrictive permissions
    chmod 600 "$SCRIPT_DIR/vault-unseal-keys.txt"
    chmod 600 "$SCRIPT_DIR/vault-root-token.txt"
    
    log_success "Vault initialized successfully"
    log_warning "🚨 CRITICAL: Backup vault-unseal-keys.txt and vault-root-token.txt securely!"
}

# Unseal Vault
unseal_vault() {
    log "🔓 Unsealing Vault..."
    
    if ! [ -f "$SCRIPT_DIR/vault-unseal-keys.txt" ]; then
        log_error "Unseal keys not found. Run initialization first."
        return 1
    fi
    
    # Use first 3 keys to unseal
    local key_count=0
    while IFS= read -r key && [ $key_count -lt 3 ]; do
        vault operator unseal "$key" >/dev/null 2>&1
        key_count=$((key_count + 1))
    done < "$SCRIPT_DIR/vault-unseal-keys.txt"
    
    log_success "Vault unsealed successfully"
}

# Authenticate with root token
authenticate_vault() {
    log "🎫 Authenticating with Vault..."
    
    if ! [ -f "$SCRIPT_DIR/vault-root-token.txt" ]; then
        log_error "Root token not found"
        return 1
    fi
    
    local root_token=$(cat "$SCRIPT_DIR/vault-root-token.txt")
    vault auth "$root_token" >/dev/null 2>&1
    
    log_success "Authenticated with Vault"
}

# Setup banking-specific secret engines
setup_secret_engines() {
    log "🏦 Setting up banking secret engines..."
    
    # Enable KV v2 secret engine for banking secrets
    vault secrets enable -version=2 -path=banking-secrets kv
    
    # Enable database secret engine for dynamic database credentials
    vault secrets enable -path=banking-db database
    
    # Enable PKI for certificate management
    vault secrets enable -path=banking-pki pki
    
    # Enable transit for encryption-as-a-service
    vault secrets enable -path=banking-transit transit
    
    # Configure PKI
    vault secrets tune -max-lease-ttl=87600h banking-pki
    
    # Generate root CA
    vault write banking-pki/root/generate/internal \
        common_name="Enterprise Banking Root CA" \
        ttl=87600h
    
    # Configure PKI URLs
    vault write banking-pki/config/urls \
        issuing_certificates="$VAULT_ADDR/v1/banking-pki/ca" \
        crl_distribution_points="$VAULT_ADDR/v1/banking-pki/crl"
    
    # Create encryption key for PII data
    vault write banking-transit/keys/pii-encryption type=aes256-gcm96
    
    # Create encryption key for payment data
    vault write banking-transit/keys/payment-encryption type=aes256-gcm96
    
    log_success "Banking secret engines configured"
}

# Setup database dynamic secrets
setup_database_secrets() {
    log "🗄️ Setting up database dynamic secrets..."
    
    # Configure PostgreSQL connection
    vault write banking-db/config/banking-postgresql \
        plugin_name=postgresql-database-plugin \
        connection_url="postgresql://{{username}}:{{password}}@postgres:5432/banking_db?sslmode=require" \
        allowed_roles="banking-app,banking-readonly,banking-admin" \
        username="vault_admin" \
        password="$VAULT_DB_ADMIN_PASSWORD"
    
    # Create role for application
    vault write banking-db/roles/banking-app \
        db_name=banking-postgresql \
        creation_statements="CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}'; \
                           GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA banking_core TO \"{{name}}\"; \
                           GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA banking_customer TO \"{{name}}\"; \
                           GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA banking_loan TO \"{{name}}\"; \
                           GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA banking_payment TO \"{{name}}\";" \
        default_ttl="1h" \
        max_ttl="24h"
    
    # Create read-only role
    vault write banking-db/roles/banking-readonly \
        db_name=banking-postgresql \
        creation_statements="CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}'; \
                           GRANT SELECT ON ALL TABLES IN SCHEMA banking_core TO \"{{name}}\"; \
                           GRANT SELECT ON ALL TABLES IN SCHEMA banking_customer TO \"{{name}}\"; \
                           GRANT SELECT ON ALL TABLES IN SCHEMA banking_loan TO \"{{name}}\"; \
                           GRANT SELECT ON ALL TABLES IN SCHEMA banking_payment TO \"{{name}}\";" \
        default_ttl="8h" \
        max_ttl="24h"
    
    log_success "Database dynamic secrets configured"
}

# Setup authentication methods
setup_auth_methods() {
    log "🔐 Setting up authentication methods..."
    
    # Enable Kubernetes auth for service authentication
    vault auth enable kubernetes
    
    # Enable AppRole for service-to-service authentication
    vault auth enable approle
    
    # Enable JWT/OIDC for external authentication
    vault auth enable jwt
    
    # Configure Kubernetes auth
    vault write auth/kubernetes/config \
        token_reviewer_jwt="$(cat /var/run/secrets/kubernetes.io/serviceaccount/token)" \
        kubernetes_host="https://kubernetes.default.svc:443" \
        kubernetes_ca_cert="$(cat /var/run/secrets/kubernetes.io/serviceaccount/ca.crt)"
    
    log_success "Authentication methods configured"
}

# Setup policies
setup_policies() {
    log "📋 Setting up banking policies..."
    
    # Customer service policy
    vault policy write banking-customer-service - <<EOF
# Customer service policy
path "banking-secrets/data/customer-service/*" {
  capabilities = ["read"]
}

path "banking-db/creds/banking-app" {
  capabilities = ["read"]
}

path "banking-transit/encrypt/pii-encryption" {
  capabilities = ["update"]
}

path "banking-transit/decrypt/pii-encryption" {
  capabilities = ["update"]
}

path "auth/token/renew-self" {
  capabilities = ["update"]
}
EOF

    # Loan service policy
    vault policy write banking-loan-service - <<EOF
# Loan service policy
path "banking-secrets/data/loan-service/*" {
  capabilities = ["read"]
}

path "banking-db/creds/banking-app" {
  capabilities = ["read"]
}

path "banking-pki/issue/banking-services" {
  capabilities = ["update"]
}

path "auth/token/renew-self" {
  capabilities = ["update"]
}
EOF

    # Payment service policy
    vault policy write banking-payment-service - <<EOF
# Payment service policy
path "banking-secrets/data/payment-service/*" {
  capabilities = ["read"]
}

path "banking-db/creds/banking-app" {
  capabilities = ["read"]
}

path "banking-transit/encrypt/payment-encryption" {
  capabilities = ["update"]
}

path "banking-transit/decrypt/payment-encryption" {
  capabilities = ["update"]
}

path "auth/token/renew-self" {
  capabilities = ["update"]
}
EOF

    # Admin policy for ops team
    vault policy write banking-admin - <<EOF
# Banking admin policy
path "banking-secrets/*" {
  capabilities = ["create", "read", "update", "delete", "list"]
}

path "banking-db/*" {
  capabilities = ["read", "list"]
}

path "banking-pki/*" {
  capabilities = ["read", "list"]
}

path "sys/health" {
  capabilities = ["read"]
}

path "sys/metrics" {
  capabilities = ["read"]
}
EOF

    log_success "Banking policies configured"
}

# Store initial secrets
store_initial_secrets() {
    log "🗝️  Storing initial banking secrets..."
    
    # Generate secure random passwords
    local jwt_secret=$(openssl rand -base64 32)
    local encryption_key=$(openssl rand -base64 32)
    local api_key=$(openssl rand -hex 32)
    
    # Store application secrets
    vault kv put banking-secrets/customer-service \
        jwt_secret="$jwt_secret" \
        encryption_key="$encryption_key" \
        external_api_key="$api_key"
    
    vault kv put banking-secrets/loan-service \
        jwt_secret="$jwt_secret" \
        ml_api_key="$(openssl rand -hex 32)" \
        risk_engine_key="$(openssl rand -base64 32)"
    
    vault kv put banking-secrets/payment-service \
        jwt_secret="$jwt_secret" \
        payment_processor_key="$(openssl rand -hex 32)" \
        encryption_key="$(openssl rand -base64 32)"
    
    vault kv put banking-secrets/open-banking \
        fapi_client_id="banking-fapi-client" \
        fapi_client_secret="$(openssl rand -base64 32)" \
        dpop_private_key="$(openssl genpkey -algorithm EC -pkcs8 -out /dev/stdout)"
    
    # Store Kafka secrets
    vault kv put banking-secrets/kafka \
        bootstrap_servers="kafka:9092" \
        security_protocol="SASL_SSL" \
        sasl_mechanism="PLAIN" \
        sasl_username="banking_producer" \
        sasl_password="$(openssl rand -base64 32)"
    
    # Store Redis secrets
    vault kv put banking-secrets/redis \
        host="redis" \
        port="6379" \
        password="$(openssl rand -base64 32)" \
        ssl_enabled="true"
    
    log_success "Initial secrets stored securely"
}

# Setup AppRole for services
setup_service_approles() {
    log "🎭 Setting up service AppRoles..."
    
    # Customer service AppRole
    vault write auth/approle/role/customer-service \
        token_policies="banking-customer-service" \
        token_ttl=1h \
        token_max_ttl=4h \
        bind_secret_id=true \
        secret_id_ttl=24h
    
    # Loan service AppRole
    vault write auth/approle/role/loan-service \
        token_policies="banking-loan-service" \
        token_ttl=1h \
        token_max_ttl=4h \
        bind_secret_id=true \
        secret_id_ttl=24h
    
    # Payment service AppRole
    vault write auth/approle/role/payment-service \
        token_policies="banking-payment-service" \
        token_ttl=1h \
        token_max_ttl=4h \
        bind_secret_id=true \
        secret_id_ttl=24h
    
    # Generate and store role IDs and secret IDs for services
    mkdir -p "$SCRIPT_DIR/service-credentials"
    
    # Customer service credentials
    vault read -field=role_id auth/approle/role/customer-service/role-id > "$SCRIPT_DIR/service-credentials/customer-service-role-id"
    vault write -field=secret_id auth/approle/role/customer-service/secret-id > "$SCRIPT_DIR/service-credentials/customer-service-secret-id"
    
    # Loan service credentials
    vault read -field=role_id auth/approle/role/loan-service/role-id > "$SCRIPT_DIR/service-credentials/loan-service-role-id"
    vault write -field=secret_id auth/approle/role/loan-service/secret-id > "$SCRIPT_DIR/service-credentials/loan-service-secret-id"
    
    # Payment service credentials
    vault read -field=role_id auth/approle/role/payment-service/role-id > "$SCRIPT_DIR/service-credentials/payment-service-role-id"
    vault write -field=secret_id auth/approle/role/payment-service/secret-id > "$SCRIPT_DIR/service-credentials/payment-service-secret-id"
    
    # Set restrictive permissions
    chmod 600 "$SCRIPT_DIR/service-credentials"/*
    
    log_success "Service AppRoles configured"
}

# Setup audit logging
setup_audit_logging() {
    log "📝 Setting up audit logging..."
    
    # Enable file audit device
    vault audit enable file file_path=/vault/logs/vault-audit.log
    
    log_success "Audit logging configured"
}

# Main execution
main() {
    export VAULT_ADDR="$VAULT_ADDR"
    export VAULT_SKIP_VERIFY=true  # Only for development
    
    wait_for_vault
    initialize_vault
    unseal_vault
    authenticate_vault
    setup_secret_engines
    setup_database_secrets
    setup_auth_methods
    setup_policies
    store_initial_secrets
    setup_service_approles
    setup_audit_logging
    
    log_success "🎉 Vault setup completed successfully!"
    log_warning "🚨 Remember to:"
    log_warning "   1. Backup unseal keys and root token securely"
    log_warning "   2. Distribute service credentials to applications"
    log_warning "   3. Enable auto-unseal in production"
    log_warning "   4. Set up Vault HA cluster"
    log_warning "   5. Configure backup strategy"
}

# Execute main function
main "$@"