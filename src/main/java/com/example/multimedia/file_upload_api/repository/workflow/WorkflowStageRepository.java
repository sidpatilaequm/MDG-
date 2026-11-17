package com.example.multimedia.file_upload_api.repository.workflow;

import com.example.multimedia.file_upload_api.entity.workflow.WorkflowStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowStageRepository extends JpaRepository<WorkflowStage, Long> {

    List<WorkflowStage> findByWorkflow_IdOrderByStageOrderAsc(Long workflowId);
}
