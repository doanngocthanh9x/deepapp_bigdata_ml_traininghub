package com.deepapp.vn.io.AA.A0.AAA0_0104.repository;

import com.deepapp.vn.io.AA.A0.AAA0_0104.entity.PageEntity;
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
 * Repository cho bảng pages
 */
@Repository("aaa0PageRepository")
public class PageRepository {

    private final JdbcTemplate jdbcTemplate;

    public PageRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private final RowMapper<PageEntity> pageRowMapper = (rs, rowNum) -> {
        PageEntity entity = new PageEntity();
        entity.setId(rs.getInt("id"));
        entity.setTaskId(rs.getInt("task_id"));
        entity.setPageNumber(rs.getInt("page_number"));
        entity.setFilePath(rs.getString("file_path"));
        entity.setOriginalWidth(rs.getInt("width"));
        entity.setOriginalHeight(rs.getInt("height"));
        return entity;
    };

    /**
     * Tìm page theo ID
     */
    public Optional<PageEntity> findById(int id) {
        String sql = "SELECT * FROM pages WHERE id = ?";
        List<PageEntity> results = jdbcTemplate.query(sql, pageRowMapper, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Lấy tất cả pages của một file
     */
    public List<PageEntity> findByTaskId(int taskId) {
        String sql = "SELECT * FROM pages WHERE task_id = ? ORDER BY page_number ASC";
        return jdbcTemplate.query(sql, pageRowMapper, taskId);
    }

    /**
     * Lấy page theo task_id và page_number
     */
    public Optional<PageEntity> findByTaskIdAndPageNumber(int taskId, int pageNumber) {
        String sql = "SELECT * FROM pages WHERE task_id = ? AND page_number = ?";
        List<PageEntity> results = jdbcTemplate.query(sql, pageRowMapper, taskId, pageNumber);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    /**
     * Lấy tất cả pages
     */
    public List<PageEntity> findAll() {
        String sql = "SELECT * FROM pages ORDER BY task_id, page_number";
        return jdbcTemplate.query(sql, pageRowMapper);
    }

    /**
     * Thêm page mới
     */
    public PageEntity insert(PageEntity page) {
        String sql = "INSERT INTO pages (task_id, page_number, file_path, width, height) " +
                     "VALUES (?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, page.getTaskId());
            ps.setInt(2, page.getPageNumber());
            ps.setString(3, page.getFilePath());
            ps.setInt(4, page.getOriginalWidth());
            ps.setInt(5, page.getOriginalHeight());
            return ps;
        }, keyHolder);

        if (keyHolder.getKey() != null) {
            page.setId(keyHolder.getKey().intValue());
        }

        return page;
    }

    /**
     * Cập nhật page
     */
    public void update(PageEntity page) {
        String sql = "UPDATE pages SET task_id = ?, page_number = ?, file_path = ?, " +
                     "original_width = ?, original_height = ? WHERE id = ?";

        jdbcTemplate.update(sql,
            page.getTaskId(),
            page.getPageNumber(),
            page.getFilePath(),
            page.getOriginalWidth(),
            page.getOriginalHeight(),
            page.getId()
        );
    }

    /**
     * Xóa page
     */
    public void delete(int id) {
        String sql = "DELETE FROM pages WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    /**
     * Xóa tất cả pages của một task
     */
    public void deleteByTaskId(int taskId) {
        String sql = "DELETE FROM pages WHERE task_id = ?";
        jdbcTemplate.update(sql, taskId);
    }

    /**
     * Đếm tổng số pages
     */
    public int count() {
        String sql = "SELECT COUNT(*) FROM pages";
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }

    /**
     * Đếm pages theo task_id
     */
    public int countByTaskId(int taskId) {
        String sql = "SELECT COUNT(*) FROM pages WHERE task_id = ?";
        return jdbcTemplate.queryForObject(sql, Integer.class, taskId);
    }
}