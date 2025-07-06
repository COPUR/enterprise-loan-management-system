#!/bin/bash

# FAPI 2.0 + DPoP Test Client Key Generation Script
# Generates private keys and certificates for test clients

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
KEYS_DIR="$SCRIPT_DIR"
CERTS_DIR="$SCRIPT_DIR/../certs"

# Create directories
mkdir -p "$KEYS_DIR"
mkdir -p "$CERTS_DIR"

echo "🔐 Generating FAPI 2.0 + DPoP Test Client Keys and Certificates..."

# Function to generate EC P-256 key pair
generate_ec_keypair() {
    local client_name=$1
    echo "  Generating EC P-256 key pair for $client_name..."
    
    # Generate private key
    openssl ecparam -genkey -name prime256v1 -noout -out "$KEYS_DIR/${client_name}-ec-private.pem"
    
    # Generate public key
    openssl ec -in "$KEYS_DIR/${client_name}-ec-private.pem" -pubout -out "$KEYS_DIR/${client_name}-ec-public.pem"
    
    # Generate self-signed certificate
    openssl req -new -x509 -key "$KEYS_DIR/${client_name}-ec-private.pem" \
        -out "$CERTS_DIR/${client_name}-ec-cert.pem" \
        -days 365 \
        -subj "/C=US/ST=NY/L=New York/O=Banking Corp/OU=IT/CN=${client_name}.banking.local"
    
    # Generate JWKS
    python3 -c "
import json
import base64
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.hazmat.backends import default_backend
import hashlib

# Load private key
with open('$KEYS_DIR/${client_name}-ec-private.pem', 'rb') as f:
    private_key = serialization.load_pem_private_key(f.read(), None, default_backend())

public_key = private_key.public_key()
public_numbers = public_key.public_numbers()

# Extract coordinates
x = public_numbers.x
y = public_numbers.y

# Convert to bytes (32 bytes for P-256)
x_bytes = x.to_bytes(32, 'big')
y_bytes = y.to_bytes(32, 'big')

# Base64url encode
x_b64 = base64.urlsafe_b64encode(x_bytes).decode().rstrip('=')
y_b64 = base64.urlsafe_b64encode(y_bytes).decode().rstrip('=')

# Calculate thumbprint
jwk_dict = {
    'crv': 'P-256',
    'kty': 'EC',
    'x': x_b64,
    'y': y_b64
}

# Create canonical JSON for thumbprint
canonical = json.dumps(jwk_dict, sort_keys=True, separators=(',', ':'))
thumbprint = base64.urlsafe_b64encode(hashlib.sha256(canonical.encode()).digest()).decode().rstrip('=')

# Create full JWK
jwk = {
    'kty': 'EC',
    'use': 'sig',
    'crv': 'P-256',
    'kid': '${client_name}-ec-key-2025',
    'x': x_b64,
    'y': y_b64,
    'alg': 'ES256'
}

# Create JWKS
jwks = {
    'keys': [jwk]
}

# Save JWKS
with open('$KEYS_DIR/${client_name}-jwks.json', 'w') as f:
    json.dump(jwks, f, indent=2)

# Save individual JWK
with open('$KEYS_DIR/${client_name}-jwk.json', 'w') as f:
    json.dump(jwk, f, indent=2)

print(f'JKT Thumbprint for ${client_name}: {thumbprint}')
"
}

# Function to generate RSA key pair
generate_rsa_keypair() {
    local client_name=$1
    echo "  Generating RSA 2048 key pair for $client_name..."
    
    # Generate private key
    openssl genrsa -out "$KEYS_DIR/${client_name}-rsa-private.pem" 2048
    
    # Generate public key
    openssl rsa -in "$KEYS_DIR/${client_name}-rsa-private.pem" -pubout -out "$KEYS_DIR/${client_name}-rsa-public.pem"
    
    # Generate self-signed certificate
    openssl req -new -x509 -key "$KEYS_DIR/${client_name}-rsa-private.pem" \
        -out "$CERTS_DIR/${client_name}-rsa-cert.pem" \
        -days 365 \
        -subj "/C=US/ST=NY/L=New York/O=Banking Corp/OU=IT/CN=${client_name}.banking.local"
    
    # Generate JWKS for RSA
    python3 -c "
import json
import base64
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.backends import default_backend
import hashlib

# Load private key
with open('$KEYS_DIR/${client_name}-rsa-private.pem', 'rb') as f:
    private_key = serialization.load_pem_private_key(f.read(), None, default_backend())

public_key = private_key.public_key()
public_numbers = public_key.public_numbers()

# Get modulus and exponent
n = public_numbers.n
e = public_numbers.e

# Convert to bytes
n_bytes = n.to_bytes((n.bit_length() + 7) // 8, 'big')
e_bytes = e.to_bytes((e.bit_length() + 7) // 8, 'big')

# Base64url encode
n_b64 = base64.urlsafe_b64encode(n_bytes).decode().rstrip('=')
e_b64 = base64.urlsafe_b64encode(e_bytes).decode().rstrip('=')

# Calculate thumbprint
jwk_dict = {
    'e': e_b64,
    'kty': 'RSA',
    'n': n_b64
}

# Create canonical JSON for thumbprint
canonical = json.dumps(jwk_dict, sort_keys=True, separators=(',', ':'))
thumbprint = base64.urlsafe_b64encode(hashlib.sha256(canonical.encode()).digest()).decode().rstrip('=')

# Create full JWK
jwk = {
    'kty': 'RSA',
    'use': 'sig',
    'kid': '${client_name}-rsa-key-2025',
    'n': n_b64,
    'e': e_b64,
    'alg': 'RS256'
}

# Create JWKS
jwks = {
    'keys': [jwk]
}

# Save JWKS
with open('$KEYS_DIR/${client_name}-rsa-jwks.json', 'w') as f:
    json.dump(jwks, f, indent=2)

# Save individual JWK
with open('$KEYS_DIR/${client_name}-rsa-jwk.json', 'w') as f:
    json.dump(jwk, f, indent=2)

print(f'JKT Thumbprint for ${client_name} (RSA): {thumbprint}')
"
}

# Generate keys for production client
echo "🏦 Generating keys for fapi2-banking-app-production..."
generate_ec_keypair "fapi2-banking-app-production"
generate_rsa_keypair "fapi2-banking-app-production"

# Generate keys for staging client
echo "🧪 Generating keys for fapi2-banking-app-staging..."
generate_ec_keypair "fapi2-banking-app-staging"
generate_rsa_keypair "fapi2-banking-app-staging"

# Generate keys for mobile client
echo "📱 Generating keys for fapi2-mobile-banking-app..."
generate_ec_keypair "fapi2-mobile-banking-app"

# Generate keys for corporate client
echo "🏢 Generating keys for fapi2-corporate-banking-client..."
generate_ec_keypair "fapi2-corporate-banking-client"
generate_rsa_keypair "fapi2-corporate-banking-client"

# Set appropriate permissions
chmod 600 "$KEYS_DIR"/*-private.pem
chmod 644 "$KEYS_DIR"/*-public.pem
chmod 644 "$KEYS_DIR"/*-jwks.json
chmod 644 "$KEYS_DIR"/*-jwk.json
chmod 644 "$CERTS_DIR"/*-cert.pem

echo "✅ All test client keys and certificates generated successfully!"
echo ""
echo "📁 Files generated:"
echo "   Private Keys: $KEYS_DIR/*-private.pem"
echo "   Public Keys:  $KEYS_DIR/*-public.pem" 
echo "   JWKS:         $KEYS_DIR/*-jwks.json"
echo "   Certificates: $CERTS_DIR/*-cert.pem"
echo ""
echo "🔒 Private keys have been secured with 600 permissions"
echo "📋 Use the generated JWKS files to configure your FAPI 2.0 clients"