package com.example.multimedia.file_upload_api.service.workflow;

import com.example.multimedia.file_upload_api.entity.workflow.*;
import com.example.multimedia.file_upload_api.repository.workflow.ApprovalActionRepository;
import com.example.multimedia.file_upload_api.repository.workflow.RequestStageRepository;
import com.example.multimedia.file_upload_api.repository.workflow.WorkflowRequestRepository;
import com.example.multimedia.file_upload_api.service.EmailService;
import com.example.multimedia.file_upload_api.service.email.EmailTemplateRenderingService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The actual stage-advancement logic: activating the next eligible stage(s), evaluating each
 * decision against the stage's voting rule, and resolving the overall request once nothing is
 * left pending. Everything about what a stage requires (members, voting rule, SLA, rejection
 * behavior) comes from the request's frozen snapshot JSON, never the live workflow tables.
 */
@Service
public class StageAdvancementService {

    private static final Logger logger = LoggerFactory.getLogger(StageAdvancementService.class);

    private final RequestStageRepository requestStageRepository;
    private final WorkflowRequestRepository workflowRequestRepository;
    private final ApprovalActionRepository approvalActionRepository;
    private final WorkflowConditionEvaluator conditionEvaluator;
    private final ApprovalActionTokenService actionTokenService;
    private final EmailTemplateRenderingService emailTemplateRenderingService;
    private final EmailService emailService;
    private final String publicBaseUrl;
    private final ApplicationEventPublisher eventPublisher;

    public StageAdvancementService(RequestStageRepository requestStageRepository,
                                    WorkflowRequestRepository workflowRequestRepository,
                                    ApprovalActionRepository approvalActionRepository,
                                    WorkflowConditionEvaluator conditionEvaluator,
                                    ApprovalActionTokenService actionTokenService,
                                    EmailTemplateRenderingService emailTemplateRenderingService,
                                    EmailService emailService,
                                    @Value("${app.public-base-url}") String publicBaseUrl,
                                    ApplicationEventPublisher eventPublisher) {
        this.requestStageRepository = requestStageRepository;
        this.workflowRequestRepository = workflowRequestRepository;
        this.approvalActionRepository = approvalActionRepository;
        this.conditionEvaluator = conditionEvaluator;
        this.actionTokenService = actionTokenService;
        this.emailTemplateRenderingService = emailTemplateRenderingService;
        this.emailService = emailService;
        this.publicBaseUrl = publicBaseUrl;
        this.eventPublisher = eventPublisher;
    }

    private JSONObject snapshotOf(WorkflowRequest request) {
        return new JSONObject(request.getWorkflowSnapshotJson());
    }

    private JSONObject stageDefOf(JSONObject snapshot, Integer stageOrder) {
        JSONArray stages = snapshot.getJSONArray("stages");
        for (int i = 0; i < stages.length(); i++) {
            JSONObject stageDef = stages.getJSONObject(i);
            if (stageDef.getInt("stageOrder") == stageOrder) return stageDef;
        }
        throw new IllegalStateException("No snapshot definition found for stageOrder " + stageOrder);
    }

    /** Activates the next WAITING stage(s) (a parallel-group batch counts as one "next step"),
     *  skipping over any whose condition doesn't match or whose approver group is empty. If
     *  every remaining stage gets skipped, the whole request resolves as approved. */
    @Transactional
    public void activateNextEligibleStages(WorkflowRequest request) {
        JSONObject snapshot = snapshotOf(request);
        JSONObject metadata = new JSONObject(
                request.getRequestMetadataJson() == null ? "{}" : request.getRequestMetadataJson());

        List<RequestStage> ordered = requestStageRepository.findByWorkflowRequest_IdOrderByStageOrderAsc(request.getId());

        for (int i = 0; i < ordered.size(); i++) {
            RequestStage stage = ordered.get(i);
            if (stage.getStatus() != RequestStageStatus.WAITING) continue;

            JSONObject stageDef = stageDefOf(snapshot, stage.getStageOrder());
            boolean conditionMatches = conditionEvaluator.matches(
                    stageDef.optString("conditionField", null),
                    stageDef.optString("conditionOp", null),
                    stageDef.optString("conditionValue", null),
                    metadata);
            JSONArray members = stageDef.getJSONArray("members");

            if (!conditionMatches || members.isEmpty()) {
                if (members.isEmpty()) {
                    logger.warn("Skipping stage '{}' on request {} - its approver group has no active members",
                            stage.getStageName(), request.getId());
                }
                stage.setStatus(RequestStageStatus.SKIPPED);
                stage.setResolvedDate(LocalDateTime.now());
                requestStageRepository.save(stage);
                continue;
            }

            // Activate this stage, plus any immediately-following stages sharing the same
            // non-null parallel group.
            List<RequestStage> batch = new ArrayList<>(List.of(stage));
            Integer parallelGroup = stageDef.has("parallelGroup") && !stageDef.isNull("parallelGroup")
                    ? stageDef.getInt("parallelGroup") : null;
            if (parallelGroup != null) {
                for (int j = i + 1; j < ordered.size(); j++) {
                    RequestStage next = ordered.get(j);
                    if (next.getStatus() != RequestStageStatus.WAITING) continue;
                    JSONObject nextDef = stageDefOf(snapshot, next.getStageOrder());
                    Integer nextGroup = nextDef.has("parallelGroup") && !nextDef.isNull("parallelGroup")
                            ? nextDef.getInt("parallelGroup") : null;
                    if (!parallelGroup.equals(nextGroup)) break;
                    batch.add(next);
                }
            }

            for (RequestStage toActivate : batch) {
                activateStage(request, toActivate, stageDefOf(snapshot, toActivate.getStageOrder()), metadata);
            }
            request.setCurrentStageOrder(stage.getStageOrder());
            workflowRequestRepository.save(request);
            return;
        }

        // Nothing left WAITING and nothing was activated - every remaining gate was skipped.
        resolveRequest(request, WorkflowRequestStatus.APPROVED);
    }

    private void activateStage(WorkflowRequest request, RequestStage stage, JSONObject stageDef, JSONObject metadata) {
        stage.setStatus(RequestStageStatus.PENDING);
        stage.setStartedDate(LocalDateTime.now());
        if (!stageDef.isNull("slaHours") && stageDef.optInt("slaHours", -1) > 0) {
            stage.setSlaDeadline(LocalDateTime.now().plusHours(stageDef.getInt("slaHours")));
        }
        requestStageRepository.save(stage);
        notifyStageMembers(request, stage, stageDef, metadata);
    }

    private void notifyStageMembers(WorkflowRequest request, RequestStage stage, JSONObject stageDef, JSONObject metadata) {
        String vendorName = metadata.optString("vendorName", request.getTitle());
        JSONArray members = stageDef.getJSONArray("members");
        for (int i = 0; i < members.length(); i++) {
            JSONObject member = members.getJSONObject(i);
            Long approverId = member.getLong("userId");
            String approverEmail = member.getString("email");
            String approveUrl = publicBaseUrl + "/api/approvals/action/"
                    + actionTokenService.generate(stage.getId(), approverId, approverEmail, ApprovalDecision.APPROVE);
            String rejectUrl = publicBaseUrl + "/api/approvals/action/"
                    + actionTokenService.generate(stage.getId(), approverId, approverEmail, ApprovalDecision.REJECT);

            Map<String, Object> vars = new HashMap<>();
            vars.put("approverName", approverEmail.contains("@") ? approverEmail.substring(0, approverEmail.indexOf('@')) : approverEmail);
            vars.put("vendorName", vendorName);
            vars.put("approveUrl", approveUrl);
            vars.put("rejectUrl", rejectUrl);

            try {
                var rendered = emailTemplateRenderingService.render("APPROVER.NEW_REQUEST", vars);
                emailService.sendHtmlEmail(approverEmail, rendered.subject(), rendered.html(), rendered.plainText());
            } catch (Exception e) {
                logger.warn("Could not send new-request notification to {}", approverEmail, e);
            }
        }
    }

    @Transactional
    public void recordAction(Long requestStageId, Long approverId, String approverEmail,
                              ApprovalDecision decision, String comment, boolean viaEmailLink) {
        RequestStage stage = requestStageRepository.findById(requestStageId)
                .orElseThrow(() -> new IllegalStateException("Approval stage not found"));

        if (stage.getStatus() != RequestStageStatus.PENDING) {
            throw new IllegalStateException("This stage has already been decided");
        }

        WorkflowRequest request = stage.getWorkflowRequest();
        JSONObject snapshot = snapshotOf(request);
        JSONObject stageDef = stageDefOf(snapshot, stage.getStageOrder());
        JSONArray members = stageDef.getJSONArray("members");

        boolean isMember = false;
        for (int i = 0; i < members.length(); i++) {
            if (members.getJSONObject(i).getLong("userId") == approverId) { isMember = true; break; }
        }
        if (!isMember) {
            throw new IllegalStateException("You are not an approver for this stage");
        }

        if (viaEmailLink && stageDef.optBoolean("requiresPortalAction", false) && decision == ApprovalDecision.APPROVE) {
            throw new IllegalStateException("This stage requires logging in to the portal to approve - the one-click link can only reject");
        }

        if (approvalActionRepository.existsByRequestStage_IdAndApproverId(requestStageId, approverId)) {
            throw new IllegalStateException("You have already acted on this stage");
        }

        String votingRule = stageDef.getString("votingRule");
        List<ApprovalAction> priorActions = approvalActionRepository.findByRequestStage_Id(requestStageId);

        if ("SEQUENTIAL".equals(votingRule)) {
            int expectedIndex = priorActions.size();
            if (expectedIndex >= members.length()
                    || members.getJSONObject(expectedIndex).getLong("userId") != approverId) {
                throw new IllegalStateException("It is not your turn to act on this stage yet");
            }
        }

        ApprovalAction action = new ApprovalAction();
        action.setRequestStage(stage);
        action.setApproverId(approverId);
        action.setApproverEmail(approverEmail);
        action.setDecision(decision);
        action.setComment(comment);
        action.setViaEmailLink(viaEmailLink);
        approvalActionRepository.save(action);

        int totalActions = priorActions.size() + 1;
        RequestStageStatus resolution = resolveStage(votingRule, decision, totalActions, members.length());
        if (resolution == null) return; // stage still pending more actions

        stage.setStatus(resolution);
        stage.setResolvedDate(LocalDateTime.now());
        requestStageRepository.save(stage);

        if (resolution == RequestStageStatus.APPROVED) {
            activateNextEligibleStages(request);
            return;
        }

        // REJECTED - what happens to the overall request depends on this stage's configured behavior.
        String rejectionBehavior = stageDef.getString("rejectionBehavior");
        switch (rejectionBehavior) {
            case "ESCALATE" -> resolveRequest(request, WorkflowRequestStatus.ESCALATED);
            case "RESTART" -> restartRequest(request);
            default -> resolveRequest(request, WorkflowRequestStatus.REJECTED); // STOP
        }
    }

    /** Null means "still pending - more actions needed before this stage resolves". */
    private RequestStageStatus resolveStage(String votingRule, ApprovalDecision decision, int totalActions, int totalMembers) {
        return switch (votingRule) {
            case "ALL" -> decision == ApprovalDecision.REJECT ? RequestStageStatus.REJECTED
                    : (totalActions >= totalMembers ? RequestStageStatus.APPROVED : null);
            case "SEQUENTIAL" -> decision == ApprovalDecision.REJECT ? RequestStageStatus.REJECTED
                    : (totalActions >= totalMembers ? RequestStageStatus.APPROVED : null);
            default -> // ANY
                    decision == ApprovalDecision.APPROVE ? RequestStageStatus.APPROVED
                            : (totalActions >= totalMembers ? RequestStageStatus.REJECTED : null);
        };
    }

    private void restartRequest(WorkflowRequest request) {
        List<RequestStage> stages = requestStageRepository.findByWorkflowRequest_IdOrderByStageOrderAsc(request.getId());
        for (RequestStage stage : stages) {
            stage.setStatus(RequestStageStatus.WAITING);
            stage.setStartedDate(null);
            stage.setResolvedDate(null);
            stage.setSlaDeadline(null);
            stage.setIsSlaBreached(false);
            stage.setReminderSent(false);
            requestStageRepository.save(stage);
        }
        request.setCurrentStageOrder(null);
        request.setStatus(WorkflowRequestStatus.PENDING);
        workflowRequestRepository.save(request);
        activateNextEligibleStages(request);
    }

    private void resolveRequest(WorkflowRequest request, WorkflowRequestStatus status) {
        request.setStatus(status);
        request.setResolvedDate(LocalDateTime.now());
        workflowRequestRepository.save(request);
        eventPublisher.publishEvent(new WorkflowRequestResolvedEvent(request.getId(), status));
    }
}
