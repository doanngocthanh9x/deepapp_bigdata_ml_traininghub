package com.deepapp.vn.io.storage.repository;

import com.deepapp.vn.io.storage.entity.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, Long> {
    
    List<TaskEntity> findByStatus(String status);
    
    List<TaskEntity> findByTaskType(String taskType);
    
    long countByStatus(String status);
}
