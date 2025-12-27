package com.deepapp.vn.io.infrastructure.cpp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Manages C++ worker lifecycle:
 * - Auto-compile C++ on startup
 * - Start C++ worker process
 * - Monitor worker health
 * - Cleanup on shutdown
 */
@Service
public class CppWorkerManager {

    private static final Logger logger = LoggerFactory.getLogger(CppWorkerManager.class);

    @Value("${cpp.worker.autoStart:true}")
    private boolean autoStart;

    @Value("${cpp.worker.autoBuild:true}")
    private boolean autoBuild;

    @Value("${cpp.worker.buildDir:build}")
    private String buildDir;

    @Value("${cpp.worker.executable:deepapp_worker_main}")
    private String executable;

    @Value("${workers.cpp.host:localhost}")
    private String grpcHost;

    @Value("${workers.cpp.port:50051}")
    private int grpcPort;

    @Value("${workers.cpp.targetId:cpp-worker}")
    private String clientId;

    private Process workerProcess;
    private Thread outputReaderThread;
    private Thread errorReaderThread;
    private volatile boolean running = false;

    @PostConstruct
    public void initialize() {
        if (!autoStart) {
            logger.info("C++ worker auto-start is disabled");
            return;
        }

        try {
            logger.info("========================================");
            logger.info("C++ Worker Manager - Initialization");
            logger.info("========================================");

            // Step 1: Auto-build C++
            if (autoBuild) {
                buildCppWorker();
            }

            // Step 2: Start worker
            startCppWorker();

            logger.info("C++ Worker Manager initialized successfully");
            logger.info("========================================");

        } catch (Exception e) {
            logger.error("Failed to initialize C++ worker manager", e);
            throw new RuntimeException("C++ worker initialization failed", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        logger.info("========================================");
        logger.info("C++ Worker Manager - Cleanup");
        logger.info("========================================");
        stopCppWorker();
        logger.info("C++ Worker Manager cleanup complete");
        logger.info("========================================");
    }

    /**
     * Build C++ worker using CMake
     */
    private void buildCppWorker() {
        try {
            logger.info("Building C++ worker...");
            
            File projectRoot = new File(System.getProperty("user.dir"));
            File buildDirectory = new File(projectRoot, buildDir);

            // Create build directory if not exists
            if (!buildDirectory.exists()) {
                logger.info("Creating build directory: {}", buildDirectory.getAbsolutePath());
                buildDirectory.mkdirs();
            }

            // Run CMake configure
            logger.info("Running CMake configure...");
            ProcessBuilder cmakeConfig = new ProcessBuilder(
                "cmake", 
                "-DCMAKE_BUILD_TYPE=Release",
                ".."
            );
            cmakeConfig.directory(buildDirectory);
            cmakeConfig.redirectErrorStream(true);

            Process configProcess = cmakeConfig.start();
            logProcessOutput(configProcess, "CMake Configure");
            int configResult = configProcess.waitFor();

            if (configResult != 0) {
                throw new RuntimeException("CMake configure failed with exit code: " + configResult);
            }

            // Run CMake build
            logger.info("Running CMake build...");
            ProcessBuilder cmakeBuild = new ProcessBuilder(
                "cmake", 
                "--build", 
                ".",
                "--config", 
                "Release",
                "-j4"
            );
            cmakeBuild.directory(buildDirectory);
            cmakeBuild.redirectErrorStream(true);

            Process buildProcess = cmakeBuild.start();
            logProcessOutput(buildProcess, "CMake Build");
            int buildResult = buildProcess.waitFor();

            if (buildResult != 0) {
                throw new RuntimeException("CMake build failed with exit code: " + buildResult);
            }

            logger.info("C++ worker built successfully");

        } catch (Exception e) {
            logger.error("Failed to build C++ worker", e);
            throw new RuntimeException("C++ build failed", e);
        }
    }

    /**
     * Start C++ worker process
     */
    private void startCppWorker() {
        try {
            logger.info("Starting C++ worker process...");
            logger.info("  gRPC Server: {}:{}", grpcHost, grpcPort);
            logger.info("  Client ID: {}", clientId);

            File projectRoot = new File(System.getProperty("user.dir"));
            File buildDirectory = new File(projectRoot, buildDir);
            File executableFile = new File(buildDirectory, executable);

            if (!executableFile.exists()) {
                throw new RuntimeException("C++ executable not found: " + executableFile.getAbsolutePath());
            }

            // Start worker process
            ProcessBuilder pb = new ProcessBuilder(
                executableFile.getAbsolutePath(),
                grpcHost + ":" + grpcPort,
                clientId
            );
            pb.directory(buildDirectory);
            pb.redirectErrorStream(false);
            
            // Inherit environment variables from parent process
            pb.environment().putAll(System.getenv());
            
            // Set DEEPAPP_PROJECT_ROOT environment variable for C++ worker if not already set
            String existingProjectRoot = System.getenv("DEEPAPP_PROJECT_ROOT");
            if (existingProjectRoot == null || existingProjectRoot.isEmpty()) {
                pb.environment().put("DEEPAPP_PROJECT_ROOT", projectRoot.getAbsolutePath());
                logger.info("Setting DEEPAPP_PROJECT_ROOT={} for C++ worker", projectRoot.getAbsolutePath());
            } else {
                logger.info("Using existing DEEPAPP_PROJECT_ROOT={} for C++ worker", existingProjectRoot);
            }

            workerProcess = pb.start();
            running = true;

            // Start threads to read output
            outputReaderThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(workerProcess.getInputStream()))) {
                    String line;
                    while (running && (line = reader.readLine()) != null) {
                        logger.info("[C++ Worker] {}", line);
                    }
                } catch (Exception e) {
                    if (running) {
                        logger.error("Error reading C++ worker output", e);
                    }
                }
            }, "cpp-worker-output");
            outputReaderThread.setDaemon(true);
            outputReaderThread.start();

            errorReaderThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(workerProcess.getErrorStream()))) {
                    String line;
                    while (running && (line = reader.readLine()) != null) {
                        logger.error("[C++ Worker ERROR] {}", line);
                    }
                } catch (Exception e) {
                    if (running) {
                        logger.error("Error reading C++ worker error stream", e);
                    }
                }
            }, "cpp-worker-error");
            errorReaderThread.setDaemon(true);
            errorReaderThread.start();

            // Wait a bit to check if process started successfully
            Thread.sleep(2000);
            if (!workerProcess.isAlive()) {
                throw new RuntimeException("C++ worker process terminated immediately");
            }

            logger.info("C++ worker process started successfully (PID: {})", 
                       workerProcess.pid());

        } catch (Exception e) {
            logger.error("Failed to start C++ worker", e);
            throw new RuntimeException("C++ worker start failed", e);
        }
    }

    /**
     * Stop C++ worker process
     */
    private void stopCppWorker() {
        if (workerProcess == null) {
            return;
        }

        try {
            logger.info("Stopping C++ worker process (PID: {})...", workerProcess.pid());
            running = false;

            // Try graceful shutdown first
            workerProcess.destroy();
            boolean terminated = workerProcess.waitFor(5, TimeUnit.SECONDS);

            if (!terminated) {
                logger.warn("C++ worker didn't stop gracefully, forcing termination...");
                workerProcess.destroyForcibly();
                workerProcess.waitFor(2, TimeUnit.SECONDS);
            }

            // Wait for threads to finish
            if (outputReaderThread != null) {
                outputReaderThread.join(1000);
            }
            if (errorReaderThread != null) {
                errorReaderThread.join(1000);
            }

            logger.info("C++ worker process stopped");

        } catch (Exception e) {
            logger.error("Error stopping C++ worker", e);
        } finally {
            workerProcess = null;
            outputReaderThread = null;
            errorReaderThread = null;
        }
    }

    /**
     * Check if C++ worker is running
     */
    public boolean isWorkerRunning() {
        return workerProcess != null && workerProcess.isAlive();
    }

    /**
     * Get worker process info
     */
    public String getWorkerInfo() {
        if (workerProcess == null) {
            return "Worker not started";
        }
        if (!workerProcess.isAlive()) {
            return "Worker terminated";
        }
        return String.format("Worker running (PID: %d)", workerProcess.pid());
    }

    /**
     * Restart C++ worker
     */
    public void restartWorker() {
        logger.info("Restarting C++ worker...");
        stopCppWorker();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        startCppWorker();
    }

    /**
     * Helper to log process output
     */
    private void logProcessOutput(Process process, String prefix) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logger.info("[{}] {}", prefix, line);
            }
        }
    }
}
