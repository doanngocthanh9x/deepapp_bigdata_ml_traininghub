#!/bin/bash

echo "Stopping DeepApp containers..."
cd "$(dirname "$0")"
docker-compose down

echo ""
echo "DeepApp stopped successfully!"
echo ""
