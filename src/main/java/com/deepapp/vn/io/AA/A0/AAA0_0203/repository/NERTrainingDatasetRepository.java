package com.deepapp.vn.io.AA.A0.AAA0_0203.repository;

import com.deepapp.vn.io.AA.A0.AAA0_0203.entity.NERTrainingDataset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for NER Training Dataset operations
 */
@Repository
public interface NERTrainingDatasetRepository extends JpaRepository<NERTrainingDataset, Long> {

    /**
     * Find dataset by template ID
     */
    Optional<NERTrainingDataset> findByTemplateId(String templateId);

    /**
     * Find all active datasets
     */
    List<NERTrainingDataset> findByActiveTrue();

    /**
     * Find datasets with approved samples
     */
    @Query("SELECT d FROM NERTrainingDataset d WHERE d.approvedCount > 0 AND d.active = true")
    List<NERTrainingDataset> findDatasetsWithApprovedSamples();

    /**
     * Check if template ID exists
     */
    boolean existsByTemplateId(String templateId);

    /**
     * Count total approved samples across all datasets
     */
    @Query("SELECT SUM(d.approvedCount) FROM NERTrainingDataset d WHERE d.active = true")
    Long countTotalApprovedSamples();

    /**
     * Delete dataset by template ID
     */
    void deleteByTemplateId(String templateId);
}
