package com.deepapp.vn.io.infrastructure.python;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;

/**
 * Manages Python worker lifecycle:
 * - Start Python worker process on startup
 * - Monitor worker health
 * - Cleanup on shutdown
 *
 * Similar to CppWorkerManager but for Python workers
 */
@Service
public class PythonWorkerManager {

    private static final Logger logger = LoggerFactory.getLogger(PythonWorkerManager.class);

    @Value("${python.worker.autoStart:true}")
    private boolean autoStart;

    @Value("${python.worker.pythonPath:python3}")
    private String pythonPath;

    @Value("${python.worker.mainScript:src/main/python/com/deepapp/vn/io/main.py}")
    private String mainScript;

    @Value("${python.worker.workingDir:}")
    private String workingDir;

    @Value("${workers.python.host:localhost}")
    private String grpcHost;

    @Value("${workers.python.port:50052}")
    private String grpcPort;

    @Value("${workers.python.targetId:python-worker}")
    private String clientId;

    private Process workerProcess;
    private Thread outputReaderThread;
    private Thread errorReaderThread;
    private volatile boolean running = false;

    @PostConstruct
    public void initialize() {
        if (!autoStart) {
            logger.info("Python worker auto-start is disabled");
            return;
        }

        try {
            logger.info("========================================");
            logger.info("Python Worker Manager - Initialization");
            logger.info("========================================");

            // Start Python worker
            startPythonWorker();

            logger.info("Python Worker Manager initialized successfully");
            logger.info("========================================");

        } catch (Exception e) {
            logger.error("Failed to initialize Python worker manager", e);
            throw new RuntimeException("Python worker initialization failed", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        logger.info("========================================");
        logger.info("Python Worker Manager - Cleanup");
        logger.info("========================================");
        stopPythonWorker();
        logger.info("Python Worker Manager cleanup complete");
        logger.info("========================================");
    }

    /**
     * Start Python worker process
     */
    private void startPythonWorker() {
        try {
            logger.info("Starting Python worker process...");
            logger.info("  Python Path: {}", pythonPath);
            logger.info("  Main Script: {}", mainScript);
            logger.info("  Working Dir: {}", getWorkingDirectory());
            logger.info("  gRPC Server: {}:{}", grpcHost, grpcPort);
            logger.info("  Client ID: {}", clientId);

            File projectRoot = new File(System.getProperty("user.dir"));
            File scriptFile = new File(projectRoot, mainScript);

            if (!scriptFile.exists()) {
                throw new RuntimeException("Python main script not found: " + scriptFile.getAbsolutePath());
            }

            // Build command
            ProcessBuilder pb = new ProcessBuilder(
                pythonPath,
                scriptFile.getAbsolutePath(),
                "--client-id", clientId,
                "--host", grpcHost,
                "--port", grpcPort
            );

            // Set working directory
            pb.directory(new File(getWorkingDirectory()));

            // Don't redirect error stream - we'll handle both separately
            pb.redirectErrorStream(false);

            workerProcess = pb.start();
            running = true;

            // Start threads to read output
            outputReaderThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(workerProcess.getInputStream()))) {
                    String line;
                    while (running && (line = reader.readLine()) != null) {
                        logger.info("[Python Worker] {}", line);
                    }
                } catch (Exception e) {
                    if (running) {
                        logger.error("Error reading Python worker output", e);
                    }
                }
            }, "python-worker-output");
            outputReaderThread.setDaemon(true);
            outputReaderThread.start();

            errorReaderThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(workerProcess.getErrorStream()))) {
                    String line;
                    while (running && (line = reader.readLine()) != null) {
                        logger.error("[Python Worker ERROR] {}", line);
                    }
                } catch (Exception e) {
                    if (running) {
                        logger.error("Error reading Python worker error stream", e);
                    }
                }
            }, "python-worker-error");
            errorReaderThread.setDaemon(true);
            errorReaderThread.start();

            // Wait a bit to check if process started successfully
            Thread.sleep(3000);
            if (!workerProcess.isAlive()) {
                throw new RuntimeException("Python worker process terminated immediately");
            }

            logger.info("Python worker process started successfully (PID: {})",
                       workerProcess.pid());

        } catch (Exception e) {
            logger.error("Failed to start Python worker", e);
            throw new RuntimeException("Python worker start failed", e);
        }
    }

    /**
     * Stop Python worker process
     */
    private void stopPythonWorker() {
        if (workerProcess == null) {
            return;
        }

        try {
            logger.info("Stopping Python worker process (PID: {})...", workerProcess.pid());
            running = false;

            // Try graceful shutdown first
            workerProcess.destroy();
            boolean terminated = workerProcess.waitFor(5, TimeUnit.SECONDS);

            if (!terminated) {
                logger.warn("Python worker didn't terminate gracefully, forcing shutdown...");
                workerProcess.destroyForcibly();
                workerProcess.waitFor(5, TimeUnit.SECONDS);
            }

            // Wait for reader threads
            if (outputReaderThread != null && outputReaderThread.isAlive()) {
                outputReaderThread.join(2000);
            }
            if (errorReaderThread != null && errorReaderThread.isAlive()) {
                errorReaderThread.join(2000);
            }

            logger.info("Python worker process stopped");

        } catch (Exception e) {
            logger.error("Error stopping Python worker", e);
        }
    }

    /**
     * Get working directory for Python process
     */
    private String getWorkingDirectory() {
        if (workingDir != null && !workingDir.trim().isEmpty()) {
            return workingDir;
        }
        return System.getProperty("user.dir");
    }

    /**
     * Check if Python worker is running
     */
    public boolean isWorkerRunning() {
        return workerProcess != null && workerProcess.isAlive() && running;
    }

    /**
     * Get worker process PID
     */
    public long getWorkerPid() {
        return workerProcess != null ? workerProcess.pid() : -1;
    }

    /**
     * Restart Python worker
     */
    public void restartWorker() {
        logger.info("Restarting Python worker...");
        stopPythonWorker();

        try {
            Thread.sleep(1000); // Brief pause
            startPythonWorker();
            logger.info("Python worker restarted successfully");
        } catch (Exception e) {
            logger.error("Failed to restart Python worker", e);
        }
    }
}