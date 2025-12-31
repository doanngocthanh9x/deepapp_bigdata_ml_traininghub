package com.deepapp.vn.io.AA.A0.AAA0_0104.service;

import com.deepapp.vn.io.AA.A0.AAA0_0104.dto.BBoxDTO;
import com.deepapp.vn.io.AA.A0.AAA0_0104.entity.BBoxEntity;
import com.deepapp.vn.io.AA.A0.AAA0_0104.repository.BBoxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service cho quản lý bounding boxes
 */
@Service
public class BBoxService {

    private static final Logger logger = LoggerFactory.getLogger(BBoxService.class);

    private final BBoxRepository bboxRepository;

    public BBoxService(BBoxRepository bboxRepository) {
        this.bboxRepository = bboxRepository;
    }

    /**
     * Lấy bbox theo ID
     */
    public Optional<BBoxDTO> getBBoxById(int id) {
        return bboxRepository.findById(id)
                .map(BBoxDTO::fromEntity);
    }

    /**
     * Lấy tất cả bboxes của một page
     */
    public List<BBoxDTO> getBBoxesByPageId(int pageId) {
        return bboxRepository.findByPageId(pageId).stream()
                .map(BBoxDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Lấy tất cả bboxes của một file
     */
    public List<BBoxDTO> getBBoxesByFileId(int fileId) {
        return bboxRepository.findByTaskId(fileId).stream()
                .map(BBoxDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Lấy tất cả bboxes
     */
    public List<BBoxDTO> getAllBBoxes() {
        return bboxRepository.findAll().stream()
                .map(BBoxDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Lấy bboxes theo class type
     */
    public List<BBoxDTO> getBBoxesByClass(String clazz) {
        return bboxRepository.findByClass(clazz).stream()
                .map(BBoxDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Lấy bboxes theo confidence threshold
     */
    public List<BBoxDTO> getBBoxesByConfidenceGreaterThan(double confidence) {
        return bboxRepository.findByConfidenceGreaterThan(confidence).stream()
                .map(BBoxDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Tạo bbox mới
     */
    @Transactional
    public BBoxDTO createBBox(BBoxEntity bbox) {
        logger.info("Creating new bbox for page {}: class={}, confidence={}",
                   bbox.getPageId(), bbox.getClassType(), bbox.getConfidence());
        BBoxEntity savedBBox = bboxRepository.insert(bbox);
        return BBoxDTO.fromEntity(savedBBox);
    }

    /**
     * Tạo nhiều bboxes cùng lúc
     */
    @Transactional
    public List<BBoxDTO> createBBoxes(List<BBoxEntity> bboxes) {
        return bboxes.stream()
                .map(this::createBBox)
                .collect(Collectors.toList());
    }

    /**
     * Cập nhật bbox
     */
    @Transactional
    public void updateBBox(BBoxEntity bbox) {
        logger.info("Updating bbox ID {}: page={}, class={}",
                   bbox.getId(), bbox.getPageId(), bbox.getClassType());
        bboxRepository.update(bbox);
    }

    /**
     * Xóa bbox
     */
    @Transactional
    public boolean deleteBBox(int id) {
        Optional<BBoxEntity> bboxOpt = bboxRepository.findById(id);
        if (bboxOpt.isPresent()) {
            logger.info("Deleting bbox: page {}, class {}",
                       bboxOpt.get().getPageId(), bboxOpt.get().getClassType());
            bboxRepository.delete(id);
            return true;
        }
        return false;
    }

    /**
     * Xóa tất cả bboxes của một page
     */
    @Transactional
    public void deleteBBoxesByPageId(int pageId) {
        logger.info("Deleting all bboxes for page ID: {}", pageId);
        bboxRepository.deleteByPageId(pageId);
    }

    /**
     * Xóa tất cả bboxes của một file
     */
    @Transactional
    public void deleteBBoxesByFileId(int fileId) {
        logger.info("Deleting all bboxes for file ID: {}", fileId);
        bboxRepository.deleteByTaskId(fileId);
    }

    /**
     * Lấy thống kê
     */
    public java.util.Map<String, Object> getStatistics() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("total_bboxes", bboxRepository.count());
        stats.put("text_bboxes", bboxRepository.countByClass("text"));
        stats.put("table_bboxes", bboxRepository.countByClass("table"));
        stats.put("object_bboxes", bboxRepository.countByClass("object"));
        return stats;
    }

    /**
     * Lấy bbox entity (cho internal use)
     */
    public Optional<BBoxEntity> getBBoxEntityById(int id) {
        return bboxRepository.findById(id);
    }

    /**
     * Lấy bboxes entity theo page ID (cho internal use)
     */
    public List<BBoxEntity> getBBoxEntitiesByPageId(int pageId) {
        return bboxRepository.findByPageId(pageId);
    }
}