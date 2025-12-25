package com.deepapp.vn.io.storage.repository;

import com.deepapp.vn.io.storage.entity.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {
    
    Optional<DocumentEntity> findByRequestId(String requestId);
    
    List<DocumentEntity> findByStatus(String status);
    
    List<DocumentEntity> findByFormatIgnoreCase(String format);
    
    List<DocumentEntity> findByCreatedAtBefore(LocalDateTime dateTime);
    
    @Query("SELECT d FROM DocumentEntity d WHERE d.createdAt < :cutoffDate")
    List<DocumentEntity> findOldDocuments(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("SELECT COUNT(d) FROM DocumentEntity d WHERE d.status = :status")
    long countByStatus(@Param("status") String status);
    
    @Query("SELECT d.status, COUNT(d) FROM DocumentEntity d GROUP BY d.status")
    List<Object[]> getStatusStatistics();
    
    void deleteByRequestId(String requestId);
    
    void deleteByCreatedAtBefore(LocalDateTime dateTime);
}
