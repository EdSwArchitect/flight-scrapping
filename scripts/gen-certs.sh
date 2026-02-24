#!/bin/bash
# Generate test certificates for Kafka SASL_SSL / TLS
# Output: JKS keystore/truststore and PEM cert/key files

set -e
OUTPUT_DIR="${1:-./certs}"
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

# Create JKS truststore (CA cert)
keytool -importcert -noprompt -trustcacerts -alias ca -file ca-cert.pem -keystore truststore.jks -storepass changeit

# Create JKS keystore (client cert + key)
openssl pkcs12 -export -in client-cert.pem -inkey client-key.pem -out client.p12 -name client -passout pass:changeit
keytool -importkeystore -srckeystore client.p12 -srcstoretype PKCS12 -srcstorepass changeit -destkeystore keystore.jks -deststoretype JKS -deststorepass changeit -noprompt

# Create server JKS
openssl pkcs12 -export -in server-cert.pem -inkey server-key.pem -out server.p12 -name server -passout pass:changeit
keytool -importkeystore -srckeystore server.p12 -srcstoretype PKCS12 -srcstorepass changeit -destkeystore server-keystore.jks -deststoretype JKS -deststorepass changeit -noprompt

echo "Certificates generated in $OUTPUT_DIR"
echo "PEM: ca-cert.pem, server-cert.pem, server-key.pem, client-cert.pem, client-key.pem"
echo "JKS: truststore.jks, keystore.jks, server-keystore.jks"
