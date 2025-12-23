#!/bin/bash

echo "=========================================="
echo "Testing Docker Build & Run"
echo "=========================================="

cd "$(dirname "$0")"

# Test 1: Check Dockerfile syntax
echo ""
echo "1. Checking Dockerfile..."
if [ -f "Dockerfile" ]; then
    echo "✓ Dockerfile exists"
else
    echo "✗ Dockerfile not found"
    exit 1
fi

# Test 2: Check docker-compose.yml
echo ""
echo "2. Checking docker-compose.yml..."
if [ -f "docker-compose.yml" ]; then
    echo "✓ docker-compose.yml exists"
    docker-compose config > /dev/null 2>&1
    if [ $? -eq 0 ]; then
        echo "✓ docker-compose.yml is valid"
    else
        echo "✗ docker-compose.yml has errors"
        exit 1
    fi
else
    echo "✗ docker-compose.yml not found"
    exit 1
fi

# Test 3: Check required files
echo ""
echo "3. Checking required files..."
required_files=(
    "CMakeLists.txt"
    "pom.xml"
    "src/main/cpp/com/DeepappMainApplication.cpp"
    "src/main/java/com/deepapp/vn/io/DeepappMainApplication.java"
    "src/main/resources/application.yml"
    "src/main/resources/application-docker.yml"
)

all_files_exist=true
for file in "${required_files[@]}"; do
    if [ -f "$file" ]; then
        echo "✓ $file"
    else
        echo "✗ $file not found"
        all_files_exist=false
    fi
done

if [ "$all_files_exist" = false ]; then
    echo ""
    echo "Some required files are missing!"
    exit 1
fi

echo ""
echo "=========================================="
echo "All checks passed!"
echo "=========================================="
echo ""
echo "Ready to build Docker image:"
echo "  ./docker-build.sh"
echo ""
