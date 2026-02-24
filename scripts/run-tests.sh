#!/usr/bin/env bash
# Run tests with Docker Desktop 4.x compatibility on macOS.
# Use standard socket (~/.docker/run/docker.sock) - required for Ryuk container mounts.
# Testcontainers 1.21.4+ supports Docker API 1.44 used by Docker Desktop 4.57+.
export DOCKER_HOST="unix://${HOME}/.docker/run/docker.sock"
mvn test "$@"
