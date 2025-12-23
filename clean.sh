#!/bin/bash

echo "=========================================="
echo "Cleaning DeepApp Project"
echo "=========================================="

cd "$(dirname "$0")"

# Function to remove directory/file and show result
clean_item() {
    local item=$1
    local description=$2
    
    if [ -e "$item" ]; then
        size=$(du -sh "$item" 2>/dev/null | cut -f1)
        rm -rf "$item"
        echo "✓ Removed $description ($size)"
    else
        echo "○ $description not found (already clean)"
    fi
}

echo ""
echo "Cleaning build artifacts..."
clean_item "target/" "Maven target directory"
clean_item "build/" "CMake build directory"

echo ""
echo "Cleaning logs..."
clean_item "logs/" "Application logs"
clean_item "*.log" "Root log files"
clean_item "/tmp/java_app.log" "Java app log"
clean_item "/tmp/cpp_worker.log" "C++ worker log"
clean_item "/tmp/spring_startup.log" "Spring startup log"
clean_item "/tmp/maven_build.log" "Maven build log"
clean_item "/tmp/maven_compile.log" "Maven compile log"

echo ""
echo "Cleaning temporary files..."
clean_item "*.tmp" "Temp files"
clean_item "*.swp" "Vim swap files"
clean_item "*~" "Backup files"

echo ""
echo "Cleaning IDE files..."
clean_item ".idea/" "IntelliJ IDEA files"
clean_item "*.iml" "IntelliJ module files"
clean_item ".vscode/settings.json" "VS Code settings (keeping workspace)"
clean_item ".classpath" "Eclipse classpath"
clean_item ".project" "Eclipse project"
clean_item ".settings/" "Eclipse settings"

echo ""
echo "Cleaning Maven wrapper (optional)..."
clean_item ".mvn/wrapper/maven-wrapper.jar" "Maven wrapper jar"

echo ""
echo "Cleaning test outputs..."
clean_item "src/test/resources/test-output/" "Test output directory"

echo ""
echo "=========================================="
echo "Cleaning Complete!"
echo "=========================================="

# Show remaining size
echo ""
echo "Current project size:"
du -sh . 2>/dev/null

echo ""
echo "Breakdown by directory:"
du -sh src/ pom.xml CMakeLists.txt docker-compose.yml Dockerfile 2>/dev/null

echo ""
echo "To rebuild:"
echo "  Maven:  mvn clean compile"
echo "  CMake:  mkdir build && cd build && cmake .. && make"
echo "  Docker: ./docker-build.sh"
echo ""
