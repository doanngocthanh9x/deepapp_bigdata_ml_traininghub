package com.deepapp.vn.io.AA.A0.AAA0_0104.repository;

import com.deepapp.vn.io.AA.A0.AAA0_0104.entity.FileEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

/**
 * Repository cho bảng tasks_files
 */
@Repository
public class FileRepository {

    private final JdbcTemplate jdbcTemplate;

    public FileRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<FileEntity> fileRowMapper = (rs, rowNum) -> {
        FileEntity entity = new FileEntity();
        entity.setId(rs.getInt("id"));
        entity.setFileName(rs.getString("file_name"));
        entity.setFilePath(rs.getString("file_path"));
        entity.setFileSize(rs.getLong("file_size"));
        entity.setMimeType(rs.getString("mime_type"));
        entity.setPageCount(rs.getInt("page_count"));
        entity.setStatus(rs.getString("status"));
        entity.setRequestId(rs.getString("request_id"));
        entity.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        entity.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return entity;
    };

    /**
     * Tìm file theo ID
     */
    public Optional<FileEntity> findById(int id) {
        String sql = "SELECT * FROM tasks_files WHERE id = ?";
        List<FileEntity> results = jdbcTemplate.query(sql, fileRowMapper, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Lấy tất cả files
     */
    public List<FileEntity> findAll() {
        String sql = "SELECT * FROM tasks_files ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, fileRowMapper);
    }

    /**
     * Lấy files theo status
     */
    public List<FileEntity> findByStatus(String status) {
        String sql = "SELECT * FROM tasks_files WHERE status = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, fileRowMapper, status);
    }

    /**
     * Thêm file mới
     */
    public FileEntity insert(FileEntity file) {
        String sql = "INSERT INTO tasks_files (file_name, file_path, file_size, mime_type, page_count, status, request_id, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, file.getFileName());
            ps.setString(2, file.getFilePath());
            ps.setLong(3, file.getFileSize());
            ps.setString(4, file.getMimeType());
            ps.setInt(5, file.getPageCount());
            ps.setString(6, file.getStatus());
            ps.setString(7, file.getRequestId());
            ps.setTimestamp(8, java.sql.Timestamp.valueOf(file.getCreatedAt()));
            ps.setTimestamp(9, java.sql.Timestamp.valueOf(file.getUpdatedAt()));
            return ps;
        }, keyHolder);

        if (keyHolder.getKey() != null) {
            file.setId(keyHolder.getKey().intValue());
        }

        return file;
    }

    /**
     * Cập nhật file
     */
    public void update(FileEntity file) {
        String sql = "UPDATE tasks_files SET file_name = ?, file_path = ?, file_size = ?, mime_type = ?, " +
                     "page_count = ?, status = ?, request_id = ?, updated_at = ? WHERE id = ?";

        jdbcTemplate.update(sql,
            file.getFileName(),
            file.getFilePath(),
            file.getFileSize(),
            file.getMimeType(),
            file.getPageCount(),
            file.getStatus(),
            file.getRequestId(),
            java.sql.Timestamp.valueOf(file.getUpdatedAt()),
            file.getId()
        );
    }

    /**
     * Cập nhật status
     */
    public void updateStatus(int id, String status) {
        String sql = "UPDATE tasks_files SET status = ?, updated_at = ? WHERE id = ?";
        jdbcTemplate.update(sql, status, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()), id);
    }

    /**
     * Xóa file
     */
    public void delete(int id) {
        String sql = "DELETE FROM tasks_files WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    /**
     * Đếm tổng số files
     */
    public int count() {
        String sql = "SELECT COUNT(*) FROM tasks_files";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    /**
     * Tìm file theo request ID
     */
    public Optional<FileEntity> findByRequestId(String requestId) {
        String sql = "SELECT * FROM tasks_files WHERE request_id = ?";
        List<FileEntity> results = jdbcTemplate.query(sql, fileRowMapper, requestId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Đếm files theo status
     */
    public int countByStatus(String status) {
        String sql = "SELECT COUNT(*) FROM tasks_files WHERE status = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, status);
    }
}