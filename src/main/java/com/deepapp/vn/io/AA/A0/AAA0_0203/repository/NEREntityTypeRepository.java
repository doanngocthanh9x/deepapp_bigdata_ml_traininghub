package com.deepapp.vn.io.AA.A0.AAA0_0203.repository;

import com.deepapp.vn.io.AA.A0.AAA0_0203.entity.NEREntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NEREntityTypeRepository extends JpaRepository<NEREntityType, Long> {
    
    /**
     * Find all entity types for a specific template (including global ones)
     */
    @Query("SELECT e FROM NEREntityType e WHERE (e.templateId = :templateId OR e.templateId IS NULL) AND e.active = true ORDER BY e.displayOrder, e.entityCode")
    List<NEREntityType> findByTemplateIdIncludingGlobal(@Param("templateId") String templateId);
    
    /**
     * Find all global entity types (templateId is null)
     */
    List<NEREntityType> findByTemplateIdIsNullAndActiveTrue();
    
    /**
     * Find all entity types for a specific template only
     */
    List<NEREntityType> findByTemplateIdAndActiveTrue(String templateId);
    
    /**
     * Find entity type by code and template
     */
    Optional<NEREntityType> findByTemplateIdAndEntityCode(String templateId, String entityCode);
    
    /**
     * Find global entity type by code
     */
    Optional<NEREntityType> findByTemplateIdIsNullAndEntityCode(String entityCode);
    
    /**
     * Check if entity code exists for template
     */
    boolean existsByTemplateIdAndEntityCode(String templateId, String entityCode);
    
    /**
     * Delete all entity types for a template
     */
    void deleteByTemplateId(String templateId);
}
