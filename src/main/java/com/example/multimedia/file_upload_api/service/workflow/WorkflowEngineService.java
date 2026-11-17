package com.example.multimedia.file_upload_api.service.workflow;

import com.example.multimedia.file_upload_api.entity.workflow.*;
import com.example.multimedia.file_upload_api.repository.workflow.ApproverGroupMemberRepository;
import com.example.multimedia.file_upload_api.repository.workflow.WorkflowRepository;
import com.example.multimedia.file_upload_api.repository.workflow.WorkflowRequestRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Entry point for starting a new approval run. Freezes a full snapshot of the workflow's
 * stages + approver-group members at creation time into
 * {@link WorkflowRequest#getWorkflowSnapshotJson()} - every later decision reads that
 * snapshot, never the live {@link Workflow}/{@link WorkflowStage}/{@link ApproverGroup}
 * tables, so an admin editing the workflow definition never changes a request already in
 * flight.
 */
@Service
public class WorkflowEngineService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowRequestRepository workflowRequestRepository;
    private final ApproverGroupMemberRepository approverGroupMemberRepository;
    private final StageAdvancementService stageAdvancementService;

    public WorkflowEngineService(WorkflowRepository workflowRepository,
                                  WorkflowRequestRepository workflowRequestRepository,
                                  ApproverGroupMemberRepository approverGroupMemberRepository,
                                  StageAdvancementService stageAdvancementService) {
        this.workflowRepository = workflowRepository;
        this.workflowRequestRepository = workflowRequestRepository;
        this.approverGroupMemberRepository = approverGroupMemberRepository;
        this.stageAdvancementService = stageAdvancementService;
    }

    @Transactional
    public WorkflowRequest createRequest(String workflowKey, String title, JSONObject requestMetadata, Long submitterId) {
        Workflow workflow = workflowRepository.findByWorkflowKey(workflowKey)
                .orElseThrow(() -> new IllegalStateException("No workflow configured for key: " + workflowKey
                        + " - a super_admin must exist before V8's seed migration can create it."));

        if (workflow.getStages().isEmpty()) {
            throw new IllegalStateException("Workflow " + workflowKey + " has no stages configured");
        }

        WorkflowRequest request = new WorkflowRequest();
        request.setWorkflow(workflow);
        request.setSubmitterId(submitterId);
        request.setTitle(title);
        request.setRequestMetadataJson(requestMetadata.toString());
        request.setWorkflowSnapshotJson(buildSnapshot(workflow).toString());
        request.setStatus(WorkflowRequestStatus.PENDING);

        List<RequestStage> stages = new ArrayList<>();
        for (WorkflowStage stage : workflow.getStages()) {
            RequestStage requestStage = new RequestStage();
            requestStage.setWorkflowRequest(request);
            requestStage.setStageOrder(stage.getStageOrder());
            requestStage.setStageName(stage.getName());
            requestStage.setStatus(RequestStageStatus.WAITING);
            stages.add(requestStage);
        }
        request.setStages(stages);

        request = workflowRequestRepository.save(request);
        stageAdvancementService.activateNextEligibleStages(request);
        return request;
    }

    private JSONObject buildSnapshot(Workflow workflow) {
        JSONObject snapshot = new JSONObject();
        snapshot.put("workflowKey", workflow.getWorkflowKey());
        snapshot.put("workflowName", workflow.getName());

        JSONArray stagesJson = new JSONArray();
        for (WorkflowStage stage : workflow.getStages()) {
            JSONObject stageJson = new JSONObject();
            stageJson.put("stageOrder", stage.getStageOrder());
            stageJson.put("name", stage.getName());
            stageJson.put("slaHours", stage.getSlaHours());
            stageJson.put("votingRule", stage.getVotingRule().name());
            stageJson.put("rejectionBehavior", stage.getRejectionBehavior().name());
            stageJson.put("isOptional", stage.getIsOptional());
            stageJson.put("conditionField", stage.getConditionField());
            stageJson.put("conditionOp", stage.getConditionOp());
            stageJson.put("conditionValue", stage.getConditionValue());
            stageJson.put("parallelGroup", stage.getParallelGroup());
            stageJson.put("requiresPortalAction", stage.getRequiresPortalAction());
            stageJson.put("approverGroupId", stage.getApproverGroup().getId());

            JSONArray membersJson = new JSONArray();
            approverGroupMemberRepository.findByApproverGroup_IdAndIsActiveTrue(stage.getApproverGroup().getId())
                    .forEach(member -> {
                        JSONObject memberJson = new JSONObject();
                        memberJson.put("userId", member.getUserId());
                        memberJson.put("email", member.getUserEmail());
                        membersJson.put(memberJson);
                    });
            stageJson.put("members", membersJson);

            stagesJson.put(stageJson);
        }
        snapshot.put("stages", stagesJson);
        return snapshot;
    }
}
