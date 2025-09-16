#!/bin/bash

# Load env variables
source .env

# Create required folders if not present
mkdir -p $OUTPUT_FOLDER/{logs,gc-logs,hadoop-logs,spark-logs}

echo "Building Docker image..."
docker compose build

echo "Starting container in background..."
nohup docker compose up > docker_out_${OUTPUT_FOLDER}.log 2>&1 &

echo "Running in background. Logs: docker_out_${OUTPUT_FOLDER}.log"