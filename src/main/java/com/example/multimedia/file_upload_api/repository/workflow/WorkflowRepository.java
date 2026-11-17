package com.example.multimedia.file_upload_api.repository.workflow;

import com.example.multimedia.file_upload_api.entity.workflow.Workflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkflowRepository extends JpaRepository<Workflow, Long> {

    Optional<Workflow> findByWorkflowKey(String workflowKey);
}
