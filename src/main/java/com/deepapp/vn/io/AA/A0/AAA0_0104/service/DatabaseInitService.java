package com.deepapp.vn.io.AA.A0.AAA0_0104.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Service để khởi tạo database SQLite và các bảng
 */
@Service
public class DatabaseInitService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitService.class);

    private final JdbcTemplate jdbcTemplate;

    @Value("${aaa0_0104.db.schema.path:/root/aaa0_0104_schema.sql}")
    private String schemaPath;

    @Value("${aaa0_0104.db.init.enabled:true}")
    private boolean initEnabled;

    public DatabaseInitService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Khởi tạo database khi ứng dụng start
     */
    @PostConstruct
    public void initializeDatabase() {
        if (!initEnabled) {
            logger.info("Database initialization is disabled");
            return;
        }

        try {
            logger.info("Initializing AAA0_0104 database...");

            // Đọc file schema
            String schemaSql = readSchemaFile();

            // Thực thi các câu lệnh SQL
            executeSchema(schemaSql);

            logger.info("Database initialization completed successfully");

            // Log thống kê sau khi init
            logDatabaseStats();

        } catch (Exception e) {
            logger.error("Failed to initialize database", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    /**
     * Đọc file schema SQL
     */
    private String readSchemaFile() throws IOException {
        try {
            // Thử đọc từ file system trước
            return new String(Files.readAllBytes(Paths.get(schemaPath)));
        } catch (IOException e) {
            // Nếu không có file, thử đọc từ classpath
            logger.warn("Schema file not found at {}, trying classpath", schemaPath);
            try {
                ClassPathResource resource = new ClassPathResource("aaa0_0104_schema.sql");
                return new String(resource.getInputStream().readAllBytes());
            } catch (IOException ex) {
                logger.error("Schema file not found in classpath either", ex);
                throw new IOException("Schema file not found at " + schemaPath + " or in classpath", ex);
            }
        }
    }

    /**
     * Thực thi schema SQL
     */
    private void executeSchema(String schemaSql) {
        // Tách các câu lệnh SQL (phân cách bởi dấu chấm phẩy)
        String[] statements = schemaSql.split(";");

        for (String statement : statements) {
            String trimmed = statement.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                try {
                    jdbcTemplate.execute(trimmed);
                    logger.debug("Executed SQL: {}", trimmed.substring(0, Math.min(100, trimmed.length())));
                } catch (Exception e) {
                    // Bỏ qua lỗi nếu là statement không quan trọng (như INSERT OR IGNORE)
                    if (!trimmed.toUpperCase().contains("INSERT OR IGNORE")) {
                        logger.warn("Failed to execute SQL statement: {}", trimmed, e);
                    }
                }
            }
        }
    }

    /**
     * Log thống kê database
     */
    private void logDatabaseStats() {
        try {
            Integer fileCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM tasks_files", Integer.class);
            Integer pageCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pages", Integer.class);
            Integer bboxCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM bboxes", Integer.class);

            logger.info("Database statistics:");
            logger.info("  - Files: {}", fileCount);
            logger.info("  - Pages: {}", pageCount);
            logger.info("  - BBoxes: {}", bboxCount);

        } catch (Exception e) {
            logger.warn("Failed to get database statistics", e);
        }
    }

    /**
     * Reset database (xóa tất cả dữ liệu và tạo lại schema)
     */
    public void resetDatabase() {
        logger.warn("Resetting database - this will delete all data!");

        try {
            // Xóa dữ liệu theo thứ tự foreign key
            jdbcTemplate.execute("DELETE FROM bboxes");
            jdbcTemplate.execute("DELETE FROM pages");
            jdbcTemplate.execute("DELETE FROM tasks_files");

            // Reset auto-increment counters
            jdbcTemplate.execute("DELETE FROM sqlite_sequence WHERE name IN ('tasks_files', 'pages', 'bboxes')");

            logger.info("Database reset completed");

        } catch (Exception e) {
            logger.error("Failed to reset database", e);
            throw new RuntimeException("Database reset failed", e);
        }
    }

    /**
     * Kiểm tra kết nối database
     */
    public boolean isDatabaseHealthy() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception e) {
            logger.error("Database health check failed", e);
            return false;
        }
    }
}