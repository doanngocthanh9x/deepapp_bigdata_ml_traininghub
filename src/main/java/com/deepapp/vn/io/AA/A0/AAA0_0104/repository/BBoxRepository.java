package com.deepapp.vn.io.AA.A0.AAA0_0104.repository;

import com.deepapp.vn.io.AA.A0.AAA0_0104.entity.BBoxEntity;
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
 * Repository cho bảng bboxes
 */
@Repository
public class BBoxRepository {

    private final JdbcTemplate jdbcTemplate;

    public BBoxRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<BBoxEntity> bboxRowMapper = (rs, rowNum) -> {
        BBoxEntity entity = new BBoxEntity();
        entity.setId(rs.getInt("id"));
        entity.setPageId(rs.getInt("page_id"));
        entity.setCoordinates(rs.getString("coordinates"));
        entity.setClassType(rs.getString("class"));
        entity.setConfidence(rs.getDouble("confidence"));
        entity.setFilePath(rs.getString("file_path"));
        return entity;
    };

    /**
     * Tìm bbox theo ID
     */
    public Optional<BBoxEntity> findById(int id) {
        String sql = "SELECT * FROM bboxes WHERE id = ?";
        List<BBoxEntity> results = jdbcTemplate.query(sql, bboxRowMapper, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Lấy tất cả bboxes của một page
     */
    public List<BBoxEntity> findByPageId(int pageId) {
        String sql = "SELECT * FROM bboxes WHERE page_id = ? ORDER BY confidence DESC";
        return jdbcTemplate.query(sql, bboxRowMapper, pageId);
    }

    /**
     * Lấy tất cả bboxes của một task (qua page_id)
     */
    public List<BBoxEntity> findByTaskId(int taskId) {
        String sql = "SELECT b.* FROM bboxes b " +
                     "JOIN pages p ON b.page_id = p.id " +
                     "WHERE p.task_id = ? ORDER BY p.page_number, b.confidence DESC";
        return jdbcTemplate.query(sql, bboxRowMapper, taskId);
    }

    /**
     * Lấy tất cả bboxes
     */
    public List<BBoxEntity> findAll() {
        String sql = "SELECT * FROM bboxes ORDER BY page_id, confidence DESC";
        return jdbcTemplate.query(sql, bboxRowMapper);
    }

    /**
     * Lấy bboxes theo class type
     */
    public List<BBoxEntity> findByClass(String clazz) {
        String sql = "SELECT * FROM bboxes WHERE class = ? ORDER BY confidence DESC";
        return jdbcTemplate.query(sql, bboxRowMapper, clazz);
    }

    /**
     * Lấy bboxes theo confidence threshold
     */
    public List<BBoxEntity> findByConfidenceGreaterThan(double confidence) {
        String sql = "SELECT * FROM bboxes WHERE confidence > ? ORDER BY confidence DESC";
        return jdbcTemplate.query(sql, bboxRowMapper, confidence);
    }

    /**
     * Thêm bbox mới
     */
    public BBoxEntity insert(BBoxEntity bbox) {
        String sql = "INSERT INTO bboxes (page_id, coordinates, class, confidence, file_path) " +
                     "VALUES (?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, bbox.getPageId());
            ps.setString(2, bbox.getCoordinates());
            ps.setString(3, bbox.getClassType());
            ps.setDouble(4, bbox.getConfidence());
            ps.setString(5, bbox.getFilePath());
            return ps;
        }, keyHolder);

        if (keyHolder.getKey() != null) {
            bbox.setId(keyHolder.getKey().intValue());
        }

        return bbox;
    }

    /**
     * Cập nhật bbox
     */
    public void update(BBoxEntity bbox) {
        String sql = "UPDATE bboxes SET page_id = ?, coordinates = ?, " +
                     "class = ?, confidence = ?, file_path = ? WHERE id = ?";

        jdbcTemplate.update(sql,
            bbox.getPageId(),
            bbox.getCoordinates(),
            bbox.getClassType(),
            bbox.getConfidence(),
            bbox.getFilePath(),
            bbox.getId()
        );
    }

    /**
     * Xóa bbox
     */
    public void delete(int id) {
        String sql = "DELETE FROM bboxes WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    /**
     * Xóa tất cả bboxes của một page
     */
    public void deleteByPageId(int pageId) {
        String sql = "DELETE FROM bboxes WHERE page_id = ?";
        jdbcTemplate.update(sql, pageId);
    }

    /**
     * Xóa tất cả bboxes của một task
     */
    public void deleteByTaskId(int taskId) {
        String sql = "DELETE FROM bboxes WHERE page_id IN (SELECT id FROM pages WHERE task_id = ?)";
        jdbcTemplate.update(sql, taskId);
    }

    /**
     * Đếm tổng số bboxes
     */
    public int count() {
        String sql = "SELECT COUNT(*) FROM bboxes";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    /**
     * Đếm bboxes theo page_id
     */
    public int countByPageId(int pageId) {
        String sql = "SELECT COUNT(*) FROM bboxes WHERE page_id = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, pageId);
    }

    /**
     * Đếm bboxes theo class
     */
    public int countByClass(String clazz) {
        String sql = "SELECT COUNT(*) FROM bboxes WHERE class = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, clazz);
    }
}