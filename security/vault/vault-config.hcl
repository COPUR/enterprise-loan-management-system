# Vault Configuration for Enterprise Banking
# Production-grade secrets management

ui = true
disable_mlock = false

# Storage backend with encryption
storage "postgresql" {
  connection_url = "postgres://vault:${VAULT_DB_PASSWORD}@postgres-vault:5432/vault_db?sslmode=require"
  table = "vault_kv_store"
  max_parallel = 32
}

# Listener with TLS termination
listener "tcp" {
  address = "0.0.0.0:8200"
  tls_cert_file = "/vault/certs/vault.crt"
  tls_key_file = "/vault/certs/vault.key"
  tls_min_version = "tls12"
  tls_cipher_suites = "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384"
}

# API address
api_addr = "https://vault:8200"
cluster_addr = "https://vault:8201"

# Telemetry
telemetry {
  prometheus_retention_time = "30s"
  disable_hostname = true
  enable_hostname_label = false
  unauthenticated_metrics_access = false
}

# Entropy Augmentation (production only)
entropy "seal" {
  mode = "augmentation"
}

# Auto-unseal with cloud KMS (production)
# seal "awskms" {
#   region = "us-east-1"
#   kms_key_id = "alias/vault-unseal-key"
# }

# Default lease settings
default_lease_ttl = "768h"
max_lease_ttl = "8760h"

# Security headers
raw_storage_endpoint = false
disable_clustering = false
disable_performance_standby = false

# Plugin directory
plugin_directory = "/vault/plugins"