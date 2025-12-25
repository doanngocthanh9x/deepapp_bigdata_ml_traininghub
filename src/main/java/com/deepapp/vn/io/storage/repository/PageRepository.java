package com.deepapp.vn.io.storage.repository;

import com.deepapp.vn.io.storage.entity.PageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PageRepository extends JpaRepository<PageEntity, Long> {
    
    List<PageEntity> findByRequestId(String requestId);
    
    List<PageEntity> findByRequestIdOrderByPageNumberAsc(String requestId);
    
    Optional<PageEntity> findByRequestIdAndPageNumber(String requestId, Integer pageNumber);
    
    List<PageEntity> findByStatus(String status);
    
    long countByRequestId(String requestId);
    
    void deleteByRequestId(String requestId);
}
