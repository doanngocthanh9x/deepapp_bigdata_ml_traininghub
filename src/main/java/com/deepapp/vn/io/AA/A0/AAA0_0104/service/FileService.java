package com.deepapp.vn.io.AA.A0.AAA0_0104.service;

import com.deepapp.vn.io.AA.A0.AAA0_0104.dto.FileDTO;
import com.deepapp.vn.io.AA.A0.AAA0_0104.entity.FileEntity;
import com.deepapp.vn.io.AA.A0.AAA0_0104.repository.FileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service cho quản lý files
 */
@Service
public class FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

    private final FileRepository fileRepository;

    public FileService(FileRepository fileRepository) {
        this.fileRepository = fileRepository;
    }

    /**
     * Lấy file theo ID
     */
    public Optional<FileDTO> getFileById(int id) {
        return fileRepository.findById(id)
                .map(FileDTO::fromEntity);
    }

    /**
     * Lấy tất cả files
     */
    public List<FileDTO> getAllFiles() {
        return fileRepository.findAll().stream()
                .map(FileDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Lấy files theo status
     */
    public List<FileDTO> getFilesByStatus(String status) {
        return fileRepository.findByStatus(status).stream()
                .map(FileDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Lấy file theo request ID
     */
    public Optional<FileDTO> getFileByRequestId(String requestId) {
        return fileRepository.findByRequestId(requestId)
                .map(FileDTO::fromEntity);
    }

    /**
     * Tạo file mới
     */
    @Transactional
    public FileDTO createFile(FileEntity file) {
        logger.info("Creating new file: {}", file.getFileName());
        FileEntity savedFile = fileRepository.insert(file);
        return FileDTO.fromEntity(savedFile);
    }

    /**
     * Cập nhật file
     */
    @Transactional
    public void updateFile(FileEntity file) {
        logger.info("Updating file ID {}: {}", file.getId(), file.getFileName());
        fileRepository.update(file);
    }

    /**
     * Cập nhật status của file
     */
    @Transactional
    public void updateFileStatus(int id, String status) {
        logger.info("Updating file ID {} status to: {}", id, status);
        fileRepository.updateStatus(id, status);
    }

    /**
     * Xóa file
     */
    @Transactional
    public boolean deleteFile(int id) {
        Optional<FileEntity> fileOpt = fileRepository.findById(id);
        if (fileOpt.isPresent()) {
            logger.info("Deleting file: {}", fileOpt.get().getFileName());
            fileRepository.delete(id);
            return true;
        }
        return false;
    }

    /**
     * Xóa nhiều files
     */
    @Transactional
    public int deleteFiles(List<Integer> ids) {
        int deleted = 0;
        for (Integer id : ids) {
            if (deleteFile(id)) {
                deleted++;
            }
        }
        return deleted;
    }

    /**
     * Lấy thống kê
     */
    public java.util.Map<String, Object> getStatistics() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("total_files", fileRepository.count());
        stats.put("pending_files", fileRepository.countByStatus("pending"));
        stats.put("processing_files", fileRepository.countByStatus("processing"));
        stats.put("completed_files", fileRepository.countByStatus("done"));
        stats.put("failed_files", fileRepository.countByStatus("error"));
        return stats;
    }

    /**
     * Lấy file entity (cho internal use)
     */
    public Optional<FileEntity> getFileEntityById(int id) {
        return fileRepository.findById(id);
    }
}