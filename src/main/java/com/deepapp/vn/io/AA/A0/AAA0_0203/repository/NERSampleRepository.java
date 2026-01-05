package com.deepapp.vn.io.AA.A0.AAA0_0203.repository;

import com.deepapp.vn.io.AA.A0.AAA0_0203.entity.NERSample;
import com.deepapp.vn.io.AA.A0.AAA0_0203.entity.NERTrainingDataset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for NER Sample operations
 */
@Repository
public interface NERSampleRepository extends JpaRepository<NERSample, Long> {

    /**
     * Find sample by sample ID
     */
    Optional<NERSample> findBySampleId(String sampleId);

    /**
     * Find all samples for a dataset
     */
    Page<NERSample> findByDataset(NERTrainingDataset dataset, Pageable pageable);

    /**
     * Find all samples for a dataset (list)
     */
    List<NERSample> findByDataset(NERTrainingDataset dataset);

    /**
     * Find approved samples for a dataset
     */
    Page<NERSample> findByDatasetAndApprovedTrue(NERTrainingDataset dataset, Pageable pageable);

    /**
     * Find approved samples for a dataset (list)
     */
    List<NERSample> findByDatasetAndApprovedTrue(NERTrainingDataset dataset);

    /**
     * Count samples for a dataset
     */
    long countByDataset(NERTrainingDataset dataset);

    /**
     * Count approved samples for a dataset
     */
    long countByDatasetAndApprovedTrue(NERTrainingDataset dataset);

    /**
     * Delete all samples for a dataset
     */
    void deleteByDataset(NERTrainingDataset dataset);

    /**
     * Delete samples by IDs
     */
    void deleteBySampleIdIn(List<String> sampleIds);

    /**
     * Check if sample ID exists
     */
    boolean existsBySampleId(String sampleId);

    /**
     * Find samples with annotations
     */
    @Query("SELECT s FROM NERSample s WHERE s.dataset = :dataset AND s.annotationCount > 0")
    List<NERSample> findSamplesWithAnnotations(@Param("dataset") NERTrainingDataset dataset);

    /**
     * Find samples by dataset template ID (optimized for list endpoint)
     */
    @Query("SELECT s FROM NERSample s WHERE s.dataset.templateId = :templateId")
    Page<NERSample> findByDatasetTemplateId(@Param("templateId") String templateId, Pageable pageable);
}
