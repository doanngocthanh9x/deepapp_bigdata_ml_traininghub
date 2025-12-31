package com.deepapp.vn.io.AA.A0.AAA0_0104.service;

import com.deepapp.vn.io.AA.A0.AAA0_0104.dto.PageDTO;
import com.deepapp.vn.io.AA.A0.AAA0_0104.entity.PageEntity;
import com.deepapp.vn.io.AA.A0.AAA0_0104.repository.PageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service cho quản lý pages
 */
@Service
public class PageService {

    private static final Logger logger = LoggerFactory.getLogger(PageService.class);

    private final PageRepository pageRepository;

    public PageService(@Qualifier("aaa0PageRepository") PageRepository pageRepository) {
        this.pageRepository = pageRepository;
    }

    /**
     * Lấy page theo ID
     */
    public Optional<PageDTO> getPageById(int id) {
        return pageRepository.findById(id)
                .map(PageDTO::fromEntity);
    }

    /**
     * Lấy tất cả pages của một file
     */
    public List<PageDTO> getPagesByFileId(int fileId) {
        return pageRepository.findByTaskId(fileId).stream()
                .map(PageDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Lấy page theo file ID và page number
     */
    public Optional<PageDTO> getPageByFileIdAndPageNumber(int fileId, int pageNumber) {
        return pageRepository.findByTaskIdAndPageNumber(fileId, pageNumber)
                .map(PageDTO::fromEntity);
    }

    /**
     * Lấy tất cả pages
     */
    public List<PageDTO> getAllPages() {
        return pageRepository.findAll().stream()
                .map(PageDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Tạo page mới
     */
    @Transactional
    public PageDTO createPage(PageEntity page) {
        logger.info("Creating new page for task {}: page {}", page.getTaskId(), page.getPageNumber());
        PageEntity savedPage = pageRepository.insert(page);
        return PageDTO.fromEntity(savedPage);
    }

    /**
     * Tạo nhiều pages cùng lúc
     */
    @Transactional
    public List<PageDTO> createPages(List<PageEntity> pages) {
        return pages.stream()
                .map(this::createPage)
                .collect(Collectors.toList());
    }

    /**
     * Cập nhật page
     */
    @Transactional
    public void updatePage(PageEntity page) {
        logger.info("Updating page ID {}: task {}, page {}", page.getId(), page.getTaskId(), page.getPageNumber());
        pageRepository.update(page);
    }

    /**
     * Xóa page
     */
    @Transactional
    public boolean deletePage(int id) {
        Optional<PageEntity> pageOpt = pageRepository.findById(id);
        if (pageOpt.isPresent()) {
            logger.info("Deleting page: task {}, page {}", pageOpt.get().getTaskId(), pageOpt.get().getPageNumber());
            pageRepository.delete(id);
            return true;
        }
        return false;
    }

    /**
     * Xóa tất cả pages của một file
     */
    @Transactional
    public void deletePagesByFileId(int fileId) {
        logger.info("Deleting all pages for file ID: {}", fileId);
        pageRepository.deleteByTaskId(fileId);
    }

    /**
     * Lấy thống kê
     */
    public java.util.Map<String, Object> getStatistics() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("total_pages", pageRepository.count());
        return stats;
    }

    /**
     * Lấy page entity (cho internal use)
     */
    public Optional<PageEntity> getPageEntityById(int id) {
        return pageRepository.findById(id);
    }

    /**
     * Lấy pages entity theo file ID (cho internal use)
     */
    public List<PageEntity> getPageEntitiesByFileId(int fileId) {
        return pageRepository.findByTaskId(fileId);
    }
}