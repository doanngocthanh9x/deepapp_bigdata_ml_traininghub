package com.deepapp.vn.io.AA.A0.AAA0_0203.repository;

import com.deepapp.vn.io.AA.A0.AAA0_0203.entity.NERAnnotation;
import com.deepapp.vn.io.AA.A0.AAA0_0203.entity.NERSample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * Repository for NER Annotation operations
 */
@Repository
public interface NERAnnotationRepository extends JpaRepository<NERAnnotation, Long> {

    /**
     * Find all annotations for a sample
     */
    List<NERAnnotation> findBySample(NERSample sample);

    /**
     * Find annotations by entity type
     */
    List<NERAnnotation> findByEntityType(String entityType);

    /**
     * Count annotations for a sample
     */
    long countBySample(NERSample sample);

    /**
     * Delete all annotations for a sample
     */
    void deleteBySample(NERSample sample);

    /**
     * Get entity type statistics for a dataset
     */
    @Query("SELECT a.entityType, COUNT(a) FROM NERAnnotation a " +
           "WHERE a.sample.dataset.templateId = :templateId " +
           "GROUP BY a.entityType")
    List<Object[]> countByEntityTypeForDataset(@Param("templateId") String templateId);

    /**
     * Get entity type statistics across all datasets
     */
    @Query("SELECT a.entityType, COUNT(a) FROM NERAnnotation a " +
           "WHERE a.sample.approved = true " +
           "GROUP BY a.entityType")
    List<Object[]> countByEntityTypeForApprovedSamples();

    /**
     * Find annotations overlapping with a position range
     */
    @Query("SELECT a FROM NERAnnotation a " +
           "WHERE a.sample = :sample " +
           "AND NOT (a.endPos <= :startPos OR a.startPos >= :endPos)")
    List<NERAnnotation> findOverlappingAnnotations(
        @Param("sample") NERSample sample,
        @Param("startPos") Integer startPos,
        @Param("endPos") Integer endPos
    );
}
