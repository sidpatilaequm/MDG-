package com.example.multimedia.file_upload_api.repository.workflow;

import com.example.multimedia.file_upload_api.entity.workflow.ApprovalAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalActionRepository extends JpaRepository<ApprovalAction, Long> {

    List<ApprovalAction> findByRequestStage_Id(Long requestStageId);

    boolean existsByRequestStage_IdAndApproverId(Long requestStageId, Long approverId);
}
