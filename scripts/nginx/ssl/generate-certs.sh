#!/bin/bash

# Enterprise Loan Management System - SSL Certificate Generation
# Creates self-signed certificates for development use

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CERT_DIR="$SCRIPT_DIR"

echo "🔐 Generating SSL certificates for Enterprise Loan Management System..."

# Certificate configuration
COUNTRY="US"
STATE="California"
CITY="San Francisco"
ORGANIZATION="Enterprise Banking"
ORGANIZATIONAL_UNIT="IT Department"
COMMON_NAME="localhost"
EMAIL="admin@banking.local"

# Generate private key
echo "🔑 Generating private key..."
openssl genrsa -out "$CERT_DIR/banking.key" 2048

# Generate certificate signing request
echo "📝 Generating certificate signing request..."
openssl req -new -key "$CERT_DIR/banking.key" -out "$CERT_DIR/banking.csr" -subj "/C=$COUNTRY/ST=$STATE/L=$CITY/O=$ORGANIZATION/OU=$ORGANIZATIONAL_UNIT/CN=$COMMON_NAME/emailAddress=$EMAIL"

# Generate self-signed certificate
echo "📜 Generating self-signed certificate..."
openssl x509 -req -in "$CERT_DIR/banking.csr" -signkey "$CERT_DIR/banking.key" -out "$CERT_DIR/banking.crt" -days 365 -extensions v3_req -extfile <(
cat <<EOF
[req]
distinguished_name = req_distinguished_name
req_extensions = v3_req
prompt = no

[req_distinguished_name]
C = $COUNTRY
ST = $STATE
L = $CITY
O = $ORGANIZATION
OU = $ORGANIZATIONAL_UNIT
CN = $COMMON_NAME
emailAddress = $EMAIL

[v3_req]
keyUsage = keyEncipherment, dataEncipherment
extendedKeyUsage = serverAuth
subjectAltName = @alt_names

[alt_names]
DNS.1 = localhost
DNS.2 = *.localhost
DNS.3 = banking.local
DNS.4 = *.banking.local
DNS.5 = api.banking.local
DNS.6 = auth.banking.local
DNS.7 = monitor.banking.local
IP.1 = 127.0.0.1
IP.2 = ::1
EOF
)

# Set proper permissions
chmod 600 "$CERT_DIR/banking.key"
chmod 644 "$CERT_DIR/banking.crt"

# Clean up CSR
rm -f "$CERT_DIR/banking.csr"

echo "✅ SSL certificates generated successfully!"
echo "📁 Certificate files:"
echo "   - Private key: $CERT_DIR/banking.key"
echo "   - Certificate: $CERT_DIR/banking.crt"
echo "🚨 These are self-signed certificates for development use only!"
echo "   For production, use certificates from a trusted Certificate Authority."

# Verify certificate
echo "🔍 Certificate verification:"
openssl x509 -in "$CERT_DIR/banking.crt" -text -noout | grep -E "(Subject:|Issuer:|Not Before:|Not After:|DNS:|IP Address:)"

exit 0