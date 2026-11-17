package com.example.multimedia.file_upload_api.repository.workflow;

import com.example.multimedia.file_upload_api.entity.workflow.RequestStage;
import com.example.multimedia.file_upload_api.entity.workflow.RequestStageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RequestStageRepository extends JpaRepository<RequestStage, Long> {

    List<RequestStage> findByWorkflowRequest_IdOrderByStageOrderAsc(Long workflowRequestId);

    Optional<RequestStage> findByWorkflowRequest_IdAndStageOrder(Long workflowRequestId, Integer stageOrder);

    /** Candidates for the hourly escalation job: still pending, past their SLA deadline, and no
     *  reminder sent yet. */
    List<RequestStage> findByStatusAndSlaDeadlineBeforeAndReminderSentFalse(RequestStageStatus status, LocalDateTime deadline);
}
