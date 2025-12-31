#!/usr/bin/env bash
set -euo pipefail

# install_ubuntu.sh
# Reproduce the main install/build steps from the repository Dockerfile on an Ubuntu host.
# Run as root or with sudo: sudo ./scripts/install_ubuntu.sh

ONNX_VERSION="1.18.0"
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
APP_DIR="$ROOT_DIR"
VENV_DIR="/opt/venv"

# Utility: run apt with or without sudo
if [ "$EUID" -ne 0 ]; then
  SUDO='sudo'
else
  SUDO=''
fi
# Avoid interactive prompts from apt
export DEBIAN_FRONTEND=noninteractive

echo "==> Running on: $(lsb_release -ds 2>/dev/null || cat /etc/os-release | grep PRETTY_NAME | cut -d= -f2- )"

# Helper to ensure a command exists (attempt apt install if missing)
ensure_cmd() {
  local cmd="$1"; local pkg="${2:-$1}"
  if ! command -v "$cmd" >/dev/null 2>&1; then
    echo "==> '$cmd' not found. Attempting to install package '$pkg'..."
    ${SUDO} apt-get update || true
    # Use sh -c with sudo to ensure environment assignments work correctly
    if ! ${SUDO} sh -c "DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends $pkg"; then
      echo "ERROR: failed to install package '$pkg' required for command '$cmd'." >&2
      echo "You can try: sudo apt-get update && sudo apt-get install -y $pkg" >&2
      exit 1
    fi
  fi
}

echo "==> Updating apt and installing system packages..."
${SUDO} apt-get update
${SUDO} DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
  build-essential \
  cmake \
  libgrpc++-dev \
  libprotobuf-dev \
  protobuf-compiler-grpc \
  git \
  pkg-config \
  curl \
  libssl-dev \
  nlohmann-json3-dev \
  libpoppler-cpp-dev \
  libpoppler-cpp0t64 \
  libtiff-dev \
  libpng-dev \
  libopencv-dev \
  libsqlite3-dev \
  wget \
  tesseract-ocr \
  libtesseract-dev \
  python3 \
  python3-pip \
  python3-venv \
  golang-go \
  maven \
  openjdk-17-jre-headless \
  libc-ares-dev \
  libupb-dev \
  libre2-dev \
  && ${SUDO} rm -rf /var/lib/apt/lists/*

# Install ONNX Runtime
echo "==> Installing ONNX Runtime ${ONNX_VERSION}..."
cd /tmp
${SUDO} wget -q "https://github.com/microsoft/onnxruntime/releases/download/v${ONNX_VERSION}/onnxruntime-linux-x64-${ONNX_VERSION}.tgz"
${SUDO} tar -xzf "onnxruntime-linux-x64-${ONNX_VERSION}.tgz"
${SUDO} mkdir -p /usr/local/onnxruntime
${SUDO} cp -r onnxruntime-linux-x64-${ONNX_VERSION}/* /usr/local/onnxruntime/
# copy library files
${SUDO} cp /usr/local/onnxruntime/lib/libonnxruntime.so.* /usr/local/lib/ || true
${SUDO} ln -sf /usr/local/lib/$(basename /usr/local/onnxruntime/lib/libonnxruntime.so.*) /usr/local/lib/libonnxruntime.so || true
${SUDO} mkdir -p /usr/local/include/onnxruntime
${SUDO} cp -r /usr/local/onnxruntime/include/* /usr/local/include/onnxruntime/ || true
${SUDO} echo "/usr/local/lib" | ${SUDO} tee /etc/ld.so.conf.d/onnxruntime.conf > /dev/null
${SUDO} ldconfig
# cleanup
${SUDO} rm -rf onnxruntime-linux-x64-${ONNX_VERSION}* || true

# Create Python venv and install python requirements if present
if [ -f "$APP_DIR/src/main/python/requirements.txt" ]; then
  echo "==> Setting up Python virtualenv at ${VENV_DIR} and installing requirements..."
  ${SUDO} python3 -m venv ${VENV_DIR}
  ${SUDO} ${VENV_DIR}/bin/pip install --upgrade pip
  ${SUDO} ${VENV_DIR}/bin/pip install --no-cache-dir -r "$APP_DIR/src/main/python/requirements.txt"
else
  echo "==> No Python requirements file found at src/main/python/requirements.txt, skipping venv setup."
fi

# Build C++ workers
if [ -f "$APP_DIR/CMakeLists.txt" ]; then
  echo "==> Building C++ workers with CMake..."

  # Ensure tools present: cmake, make, git, pkg-config, protoc
  ensure_cmd cmake cmake
  ensure_cmd make build-essential
  ensure_cmd git git
  ensure_cmd pkg-config pkg-config
  ensure_cmd protoc protobuf-compiler

  # Ensure gRPC development libraries (pkg-config module 'grpc++')
  echo "==> Ensuring gRPC development libraries are installed (libgrpc++-dev, libgrpc-dev)..."
  ${SUDO} sh -c "DEBIAN_FRONTEND=noninteractive apt-get install -y libgrpc++-dev libgrpc-dev || true"
  ${SUDO} ldconfig || true

  # Verify pkg-config can find grpc++
  if ! pkg-config --exists grpc++; then
    echo "ERROR: pkg-config cannot find 'grpc++'." >&2
    echo "Try installing: sudo apt-get update && sudo apt-get install -y libgrpc++-dev libgrpc-dev pkg-config" >&2
    echo "After installing, re-run this script. See $APP_DIR/build/CMakeFiles/CMakeOutput.log for details." >&2 || true
    exit 1
  else
    echo "==> Found gRPC lib via pkg-config: $(pkg-config --modversion grpc++ 2>/dev/null || echo 'version unknown')"
  fi

  # Ensure proto_utils and other grpc 'impl' wrapper headers are present in /usr/include or /usr/local
  echo "==> Ensuring grpc impl wrapper headers are present (so generated headers can include <grpcpp/impl/*.h>)"
  GRPC_IMPL_HEADERS=(proto_utils.h rpc_method.h server_callback_handlers.h service_type.h)
  for hdr in "${GRPC_IMPL_HEADERS[@]}"; do
    # grpcpp path
    if [ -f "/usr/include/grpcpp/impl/codegen/$hdr" ] && [ ! -f "/usr/include/grpcpp/impl/$hdr" ]; then
      ${SUDO} mkdir -p /usr/include/grpcpp/impl || true
      echo "#pragma once\n#include \"grpcpp/impl/codegen/$hdr\"" | ${SUDO} tee /usr/include/grpcpp/impl/$hdr > /dev/null || true
      echo "==> Created wrapper /usr/include/grpcpp/impl/$hdr"
    fi
    if [ -f "/usr/local/include/grpcpp/impl/codegen/$hdr" ] && [ ! -f "/usr/local/include/grpcpp/impl/$hdr" ]; then
      ${SUDO} mkdir -p /usr/local/include/grpcpp/impl || true
      echo "#pragma once\n#include \"grpcpp/impl/codegen/$hdr\"" | ${SUDO} tee /usr/local/include/grpcpp/impl/$hdr > /dev/null || true
      echo "==> Created wrapper /usr/local/include/grpcpp/impl/$hdr"
    fi
    # grpc++ path (alternate namespace)
    if [ -f "/usr/include/grpc++/impl/codegen/$hdr" ] && [ ! -f "/usr/include/grpc++/impl/$hdr" ]; then
      ${SUDO} mkdir -p /usr/include/grpc++/impl || true
      echo "#pragma once\n#include \"grpc++/impl/codegen/$hdr\"" | ${SUDO} tee /usr/include/grpc++/impl/$hdr > /dev/null || true
      echo "==> Created wrapper /usr/include/grpc++/impl/$hdr"
    fi
    if [ -f "/usr/local/include/grpc++/impl/codegen/$hdr" ] && [ ! -f "/usr/local/include/grpc++/impl/$hdr" ]; then
      ${SUDO} mkdir -p /usr/local/include/grpc++/impl || true
      echo "#pragma once\n#include \"grpc++/impl/codegen/$hdr\"" | ${SUDO} tee /usr/local/include/grpc++/impl/$hdr > /dev/null || true
      echo "==> Created wrapper /usr/local/include/grpc++/impl/$hdr"
    fi
  done

  # Ensure the gRPC C++ plugin binary (grpc_cpp_plugin) exists for Protobuf generation
  if command -v grpc_cpp_plugin >/dev/null 2>&1; then
    echo "==> Found grpc_cpp_plugin at $(command -v grpc_cpp_plugin)"
  else
    echo "==> grpc_cpp_plugin not found. Attempting to install via apt packages (may not provide plugin)..."
    ${SUDO} sh -c "DEBIAN_FRONTEND=noninteractive apt-get install -y grpc grpc-tools || true"
    # If still missing, build grpc_cpp_plugin from source (fallback)
    if ! command -v grpc_cpp_plugin >/dev/null 2>&1; then
      echo "==> Building grpc_cpp_plugin from source (this may take several minutes)..."
      TMPGRPC="/tmp/grpc_build_$$"
      rm -rf "$TMPGRPC" || true
      git clone --depth 1 -b v1.54.0 https://github.com/grpc/grpc.git "$TMPGRPC" || { echo "ERROR: git clone failed" >&2; exit 1; }
      pushd "$TMPGRPC" >/dev/null || exit 1
      git submodule update --init --recursive || true
      mkdir -p cmake/build && pushd cmake/build >/dev/null || exit 1
      cmake ../.. || { echo "ERROR: cmake configure for grpc failed" >&2; popd >/dev/null; popd >/dev/null; exit 1; }
      make -j$(nproc) grpc_cpp_plugin || { echo "ERROR: building grpc_cpp_plugin failed" >&2; popd >/dev/null; popd >/dev/null; exit 1; }
      # Install gRPC headers and libraries to /usr/local (so grpc headers like grpcpp/impl/proto_utils.h are available)
      echo "==> Installing gRPC to /usr/local (may require sudo)..."
      if ! ${SUDO} make install -j$(nproc); then
        echo "WARNING: 'make install' for gRPC failed. Will try to copy grpc_cpp_plugin to /usr/local/bin anyway." >&2
      fi
      # copy plugin to /usr/local/bin as fallback
      ${SUDO} cp -v grpc_cpp_plugin /usr/local/bin/ 2>/dev/null || true
      ${SUDO} ldconfig || true

      # Ensure wrapper headers exist for proto_utils.h (some distros place it in codegen/)
      echo "==> Ensuring grpc proto_utils header is available at expected paths..."
      # Prefer creating wrappers in /usr/local if needed
      if [ ! -f "/usr/local/include/grpcpp/impl/proto_utils.h" ] && [ -f "/usr/include/grpcpp/impl/codegen/proto_utils.h" ]; then
        ${SUDO} mkdir -p /usr/local/include/grpcpp/impl || true
        echo "#pragma once\n#include \"grpcpp/impl/codegen/proto_utils.h\"" | ${SUDO} tee /usr/local/include/grpcpp/impl/proto_utils.h > /dev/null || true
        echo "==> Created wrapper /usr/local/include/grpcpp/impl/proto_utils.h"
      fi
      if [ ! -f "/usr/local/include/grpc++/impl/proto_utils.h" ] && [ -f "/usr/include/grpc++/impl/codegen/proto_utils.h" ]; then
        ${SUDO} mkdir -p /usr/local/include/grpc++/impl || true
        echo "#pragma once\n#include \"grpc++/impl/codegen/proto_utils.h\"" | ${SUDO} tee /usr/local/include/grpc++/impl/proto_utils.h > /dev/null || true
        echo "==> Created wrapper /usr/local/include/grpc++/impl/proto_utils.h"
      fi
      # Also ensure wrappers exist directly in /usr/include if needed (helps compilers not searching /usr/local)
      if [ ! -f "/usr/include/grpcpp/impl/proto_utils.h" ] && [ -f "/usr/include/grpcpp/impl/codegen/proto_utils.h" ]; then
        ${SUDO} mkdir -p /usr/include/grpcpp/impl || true
        echo "#pragma once\n#include \"grpcpp/impl/codegen/proto_utils.h\"" | ${SUDO} tee /usr/include/grpcpp/impl/proto_utils.h > /dev/null || true
        echo "==> Created wrapper /usr/include/grpcpp/impl/proto_utils.h"
      fi
      if [ ! -f "/usr/include/grpc++/impl/proto_utils.h" ] && [ -f "/usr/include/grpc++/impl/codegen/proto_utils.h" ]; then
        ${SUDO} mkdir -p /usr/include/grpc++/impl || true
        echo "#pragma once\n#include \"grpc++/impl/codegen/proto_utils.h\"" | ${SUDO} tee /usr/include/grpc++/impl/proto_utils.h > /dev/null || true
        echo "==> Created wrapper /usr/include/grpc++/impl/proto_utils.h"
      fi

      popd >/dev/null || true
      popd >/dev/null || true
      rm -rf "$TMPGRPC" || true
    fi

    if ! command -v grpc_cpp_plugin >/dev/null 2>&1; then
      echo "ERROR: grpc_cpp_plugin still not found. Please install the gRPC C++ plugin (grpc_cpp_plugin) and re-run this script." >&2
      exit 1
    fi
    echo "==> Found grpc_cpp_plugin at $(command -v grpc_cpp_plugin)"
  fi

  # Ensure protobuf development libraries are installed and discoverable via pkg-config
  echo "==> Ensuring protobuf development libraries (libprotobuf-dev, protobuf-compiler) are installed..."
  ${SUDO} sh -c "DEBIAN_FRONTEND=noninteractive apt-get install -y libprotobuf-dev protobuf-compiler || true"
  ${SUDO} ldconfig || true

  if ! pkg-config --exists protobuf; then
    echo "ERROR: pkg-config cannot find 'protobuf'." >&2
    echo "Try installing: sudo apt-get update && sudo apt-get install -y libprotobuf-dev protobuf-compiler pkg-config" >&2
    echo "After installing, re-run this script. See $APP_DIR/build/CMakeFiles/CMakeOutput.log for details." >&2 || true
    exit 1
  else
    echo "==> Found protobuf via pkg-config: $(pkg-config --modversion protobuf 2>/dev/null || echo 'version unknown')"
  fi

  # Check for Poppler (poppler-cpp). Try both 'poppler-cpp' and fallback 'poppler'.
  echo "==> Checking for Poppler (pkg-config name: poppler-cpp or poppler)"
  if pkg-config --exists poppler-cpp; then
    echo "==> Found poppler-cpp via pkg-config"
  elif pkg-config --exists poppler; then
    echo "==> Found poppler via pkg-config (using 'poppler')"
  else
    echo "==> poppler pkg-config not found. Attempting to install Poppler dev packages..."
    ${SUDO} sh -c "DEBIAN_FRONTEND=noninteractive apt-get install -y libpoppler-cpp-dev libpoppler-dev poppler-utils || true"
    ${SUDO} ldconfig || true
    if pkg-config --exists poppler-cpp; then
      echo "==> Now found poppler-cpp via pkg-config"
    elif pkg-config --exists poppler; then
      echo "==> Now found poppler via pkg-config (using 'poppler')"
    else
      echo "ERROR: Could not find poppler pkg-config module (poppler-cpp or poppler)." >&2
      echo "Files in pkgconfig locations (if any):" >&2
      ${SUDO} sh -c "ls -l /usr/lib/*/pkgconfig/poppler* 2>/dev/null || true" >&2 || true
      ${SUDO} sh -c "ls -l /usr/lib/pkgconfig/poppler* 2>/dev/null || true" >&2 || true
      ${SUDO} sh -c "dpkg -L libpoppler-cpp-dev 2>/dev/null || true" >&2 || true
      echo "Try installing: sudo apt-get update && sudo apt-get install -y libpoppler-cpp-dev libpoppler-dev poppler-utils" >&2
      echo "After installing, re-run this script. See $APP_DIR/build/CMakeFiles/CMakeOutput.log for details." >&2 || true
      exit 1
    fi
  fi

  # Check for libtiff (pkg-config name: libtiff-4)
  echo "==> Checking for libtiff (pkg-config name: libtiff-4)"
  if pkg-config --exists libtiff-4; then
    echo "==> Found libtiff-4 via pkg-config"
  else
    echo "==> libtiff-4 not found. Attempting to install libtiff dev packages..."
    ${SUDO} sh -c "DEBIAN_FRONTEND=noninteractive apt-get install -y libtiff-dev libtiff5-dev || true"
    ${SUDO} ldconfig || true
    if pkg-config --exists libtiff-4; then
      echo "==> Now found libtiff-4 via pkg-config"
    else
      echo "ERROR: Could not find libtiff pkg-config module 'libtiff-4'." >&2
      echo "Pkgconfig files (if any):" >&2
      ${SUDO} sh -c "ls -l /usr/lib/*/pkgconfig/libtiff* 2>/dev/null || true" >&2 || true
      ${SUDO} sh -c "ls -l /usr/lib/pkgconfig/libtiff* 2>/dev/null || true" >&2 || true
      ${SUDO} sh -c "dpkg -L libtiff-dev 2>/dev/null || true" >&2 || true
      echo "Try installing: sudo apt-get update && sudo apt-get install -y libtiff-dev" >&2
      echo "After installing, re-run this script. See $APP_DIR/build/CMakeFiles/CMakeOutput.log for details." >&2 || true
      exit 1
    fi
  fi

  # Check for libpng (pkg-config name: libpng)
  echo "==> Checking for libpng (pkg-config name: libpng)"
  if pkg-config --exists libpng; then
    echo "==> Found libpng via pkg-config"
  else
    echo "==> libpng not found. Attempting to install libpng dev packages..."
    ${SUDO} sh -c "DEBIAN_FRONTEND=noninteractive apt-get install -y libpng-dev libpng16-16 || true"
    ${SUDO} ldconfig || true
    if pkg-config --exists libpng; then
      echo "==> Now found libpng via pkg-config"
    else
      echo "ERROR: Could not find libpng pkg-config module 'libpng'." >&2
      echo "Pkgconfig files (if any):" >&2
      ${SUDO} sh -c "ls -l /usr/lib/*/pkgconfig/libpng* 2>/dev/null || true" >&2 || true
      ${SUDO} sh -c "ls -l /usr/lib/pkgconfig/libpng* 2>/dev/null || true" >&2 || true
      ${SUDO} sh -c "dpkg -L libpng-dev 2>/dev/null || true" >&2 || true
      echo "Try installing: sudo apt-get update && sudo apt-get install -y libpng-dev" >&2
      echo "After installing, re-run this script. See $APP_DIR/build/CMakeFiles/CMakeOutput.log for details." >&2 || true
      exit 1
    fi
  fi

  # Check for OpenCV CMake config (OpenCVConfig.cmake)
  echo "==> Locating OpenCV (OpenCVConfig.cmake)..."
  OPENCV_CMAKE_PATH=""
  for p in "/usr/lib/x86_64-linux-gnu/cmake/opencv4" "/usr/lib/cmake/opencv4" "/usr/local/lib/cmake/opencv4" "/usr/local/share/opencv4"; do
    if [ -f "$p/OpenCVConfig.cmake" ]; then
      OPENCV_CMAKE_PATH="$p"
      break
    fi
  done

  if [ -z "$OPENCV_CMAKE_PATH" ]; then
    echo "==> OpenCV CMake config not found. Attempting to install libopencv-dev..."
    ${SUDO} sh -c "DEBIAN_FRONTEND=noninteractive apt-get install -y libopencv-dev || true"
    ${SUDO} ldconfig || true
    for p in "/usr/lib/x86_64-linux-gnu/cmake/opencv4" "/usr/lib/cmake/opencv4" "/usr/local/lib/cmake/opencv4" "/usr/local/share/opencv4"; do
      if [ -f "$p/OpenCVConfig.cmake" ]; then
        OPENCV_CMAKE_PATH="$p"
        break
      fi
    done
  fi

  if [ -n "$OPENCV_CMAKE_PATH" ]; then
    echo "==> Found OpenCV config at $OPENCV_CMAKE_PATH"
    export OpenCV_DIR="$OPENCV_CMAKE_PATH"
  else
    echo "ERROR: OpenCV config not found (OpenCVConfig.cmake)." >&2
    echo "Try installing: sudo apt-get update && sudo apt-get install -y libopencv-dev" >&2
    echo "Installed opencv packages:" >&2
    ${SUDO} sh -c "dpkg -l | grep -i opencv || true" >&2 || true
    echo "After installing, re-run this script. See $APP_DIR/build/CMakeFiles/CMakeOutput.log for details." >&2 || true
    exit 1
  fi

  # Check for SQLite3 development headers and library
  echo "==> Checking for SQLite3 development headers and library (sqlite3.h and libsqlite3.so)"
  if [ -f "/usr/include/sqlite3.h" ] && ldconfig -p | grep -q "libsqlite3"; then
    echo "==> Found SQLite3 development files"
  else
    echo "==> SQLite3 dev files not found. Attempting to install libsqlite3-dev..."
    ${SUDO} sh -c "DEBIAN_FRONTEND=noninteractive apt-get install -y libsqlite3-dev || true"
    ${SUDO} ldconfig || true
    if [ -f "/usr/include/sqlite3.h" ] && ldconfig -p | grep -q "libsqlite3"; then
      echo "==> Now found SQLite3 development files"
    else
      echo "ERROR: Could not find SQLite3 development headers/libs (sqlite3.h / libsqlite3.so)." >&2
      echo "Pkgconfig files (if any):" >&2
      ${SUDO} sh -c "ls -l /usr/lib/*/pkgconfig/sqlite3* 2>/dev/null || true" >&2 || true
      ${SUDO} sh -c "ls -l /usr/lib/pkgconfig/sqlite3* 2>/dev/null || true" >&2 || true
      ${SUDO} sh -c "dpkg -L libsqlite3-dev 2>/dev/null || true" >&2 || true
      echo "Try installing: sudo apt-get update && sudo apt-get install -y libsqlite3-dev" >&2
      echo "After installing, re-run this script. See $APP_DIR/build/CMakeFiles/CMakeOutput.log for details." >&2 || true
      exit 1
    fi
  fi

  mkdir -p "$APP_DIR/build"
  pushd "$APP_DIR/build" > /dev/null
  export CMAKE_PREFIX_PATH=/usr/local

  # Enable gRPC callback/reactor API used by generated code (see grpc headers guarded by GRPC_CALLBACK_API_NONEXPERIMENTAL)
  export CXXFLAGS="${CXXFLAGS:-} -DGRPC_CALLBACK_API_NONEXPERIMENTAL"
  export CFLAGS="${CFLAGS:-} -DGRPC_CALLBACK_API_NONEXPERIMENTAL"
  echo "==> Exported CXXFLAGS and CFLAGS to enable gRPC callback API"

  # Persist for future shells (so 'mvn spring-boot:run' picks it up in new sessions)
  if [ -d "/etc/profile.d" ]; then
    ${SUDO} sh -c "cat > /etc/profile.d/deepapp.sh <<'PROFILE'
# Exports needed by Deepapp build (enable gRPC callback/non-experimental API)
export CXXFLAGS=\"${CXXFLAGS:-} -DGRPC_CALLBACK_API_NONEXPERIMENTAL\"
export CFLAGS=\"${CFLAGS:-} -DGRPC_CALLBACK_API_NONEXPERIMENTAL\"
PROFILE"
    ${SUDO} chmod 644 /etc/profile.d/deepapp.sh || true
    echo "==> Wrote /etc/profile.d/deepapp.sh (will apply to new shells)"
  fi

  # Pass flags explicitly to CMake configure so current run picks them up
  if ! cmake -DCMAKE_CXX_FLAGS="$CXXFLAGS" -DCMAKE_C_FLAGS="$CFLAGS" ..; then
    echo "ERROR: 'cmake' command failed. Check that cmake is installed and working (run 'cmake --version')." >&2
    echo "See: $APP_DIR/build/CMakeFiles/CMakeOutput.log and $APP_DIR/build/CMakeFiles/CMakeError.log" >&2 || true
    exit 1
  fi
  make -j$(nproc)
  popd > /dev/null
else
  echo "==> No CMakeLists.txt found at project root; skipping C++ build."
fi

# Build Java application (Maven)
if [ -f "$APP_DIR/pom.xml" ]; then
  echo "==> Building Java app with Maven..."
  pushd "$APP_DIR" > /dev/null
  mvn clean package -DskipTests
  # copy jar to project root as app.jar for convenience
  if compgen -G "target/*.jar" > /dev/null; then
    cp target/*.jar ./app.jar
    echo "==> Copied target/*.jar to ./app.jar"
  else
    echo "==> No built jar found under target/"
  fi
  popd > /dev/null
else
  echo "==> No pom.xml found; skipping Java build."
fi

# Build Go gRPC server (clone if necessary)
if [ ! -d "$APP_DIR/tmp/go_grpc_hub" ]; then
  echo "==> Cloning Go gRPC server source..."
  git clone https://gitlab.com/dnt.doanngocthanh/go_grpc_hub.git "$APP_DIR/tmp/go_grpc_hub"
fi
if [ -d "$APP_DIR/tmp/go_grpc_hub" ]; then
  echo "==> Building Go gRPC server..."
  pushd "$APP_DIR/tmp/go_grpc_hub" > /dev/null
  go mod tidy
  go build -o "$APP_DIR/grpc-server" ./cmd/server
  chmod +x "$APP_DIR/grpc-server" || true
  popd > /dev/null
fi

# Create start script similar to container start.sh
cat > "$APP_DIR/start.sh" <<'EOL'
#!/usr/bin/env bash
set -e
# Create host directories if they do not exist
mkdir -p /host/logs
mkdir -p /host/models

# Copy models to host if not exist
if [ ! -f /host/models/vietocr_onnx/cnn.onnx ]; then
    cp -r /app/config/src/main/resources/models/* /host/models/ 2>/dev/null || true
fi

# Start gRPC server
./grpc-server &
sleep 5

# Start Java app
java $JAVA_OPTS -jar app.jar
EOL
chmod +x "$APP_DIR/start.sh"

# Final notes
echo "\n==> Install/build script completed."
echo "Artifacts placed in project root (example: app.jar, grpc-server, build/deepapp_worker_main if built)."
echo "Use './start.sh' from project root to start services (make sure binaries exist and are executable)."

echo "==> Done."

exit 0
