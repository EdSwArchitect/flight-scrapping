#!/bin/bash
# Generate PEM format certificates only (no JKS)
# For use with Kafka clients that support PEM directly

set -e
OUTPUT_DIR="${1:-./certs-pem}"
mkdir -p "$OUTPUT_DIR"
cd "$OUTPUT_DIR"

# Generate CA
openssl genrsa -out ca-key.pem 2048
openssl req -new -x509 -key ca-key.pem -out ca-cert.pem -days 365 -subj "/CN=Kafka-CA"

# Generate server cert
openssl genrsa -out server-key.pem 2048
openssl req -new -key server-key.pem -out server.csr -subj "/CN=kafka"
echo "subjectAltName=DNS:kafka,DNS:localhost,IP:127.0.0.1" > server-ext.cnf
openssl x509 -req -in server.csr -CA ca-cert.pem -CAkey ca-key.pem -CAcreateserial -out server-cert.pem -days 365 -extfile server-ext.cnf

# Generate client cert
openssl genrsa -out client-key.pem 2048
openssl req -new -key client-key.pem -out client.csr -subj "/CN=military-client"
openssl x509 -req -in client.csr -CA ca-cert.pem -CAkey ca-key.pem -CAcreateserial -out client-cert.pem -days 365

rm -f server.csr client.csr server-ext.cnf *.srl

echo "PEM certificates generated in $OUTPUT_DIR"
echo "Files: ca-cert.pem, ca-key.pem, server-cert.pem, server-key.pem, client-cert.pem, client-key.pem"
