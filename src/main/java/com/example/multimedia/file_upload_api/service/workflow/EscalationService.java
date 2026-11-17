package com.example.multimedia.file_upload_api.service.workflow;

import com.example.multimedia.file_upload_api.entity.workflow.ApprovalDecision;
import com.example.multimedia.file_upload_api.entity.workflow.RequestStage;
import com.example.multimedia.file_upload_api.entity.workflow.RequestStageStatus;
import com.example.multimedia.file_upload_api.entity.workflow.WorkflowRequest;
import com.example.multimedia.file_upload_api.repository.workflow.RequestStageRepository;
import com.example.multimedia.file_upload_api.service.EmailService;
import com.example.multimedia.file_upload_api.service.email.EmailTemplateRenderingService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal v1 SLA escalation: once an hour, find pending stages past their SLA deadline that
 * haven't had a reminder sent yet, flag them breached, and send one reminder email per
 * approver. This is deliberately not the full escalate-to-next-approver behavior WorkFlow's
 * own (never actually scheduled) equivalent implements - just a real, running reminder,
 * which is more than existed in production.
 */
@Service
public class EscalationService {

    private static final Logger logger = LoggerFactory.getLogger(EscalationService.class);

    private final RequestStageRepository requestStageRepository;
    private final ApprovalActionTokenService actionTokenService;
    private final EmailTemplateRenderingService emailTemplateRenderingService;
    private final EmailService emailService;
    private final String publicBaseUrl;

    public EscalationService(RequestStageRepository requestStageRepository,
                              ApprovalActionTokenService actionTokenService,
                              EmailTemplateRenderingService emailTemplateRenderingService,
                              EmailService emailService,
                              @Value("${app.public-base-url}") String publicBaseUrl) {
        this.requestStageRepository = requestStageRepository;
        this.actionTokenService = actionTokenService;
        this.emailTemplateRenderingService = emailTemplateRenderingService;
        this.emailService = emailService;
        this.publicBaseUrl = publicBaseUrl;
    }

    @Scheduled(fixedRate = 60 * 60 * 1000) // hourly
    @Transactional
    public void sendSlaReminders() {
        List<RequestStage> breached = requestStageRepository
                .findByStatusAndSlaDeadlineBeforeAndReminderSentFalse(RequestStageStatus.PENDING, LocalDateTime.now());
        for (RequestStage stage : breached) {
            try {
                remind(stage);
            } catch (Exception e) {
                logger.warn("Failed to send SLA reminder for request stage {}", stage.getId(), e);
            }
        }
    }

    private void remind(RequestStage stage) {
        stage.setIsSlaBreached(true);
        stage.setReminderSent(true);
        requestStageRepository.save(stage);

        WorkflowRequest request = stage.getWorkflowRequest();
        JSONObject snapshot = new JSONObject(request.getWorkflowSnapshotJson());
        JSONObject metadata = new JSONObject(request.getRequestMetadataJson() == null ? "{}" : request.getRequestMetadataJson());
        String vendorName = metadata.optString("vendorName", request.getTitle());

        JSONArray stages = snapshot.getJSONArray("stages");
        JSONObject stageDef = null;
        for (int i = 0; i < stages.length(); i++) {
            JSONObject candidate = stages.getJSONObject(i);
            if (candidate.getInt("stageOrder") == stage.getStageOrder()) { stageDef = candidate; break; }
        }
        if (stageDef == null) return;

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
            vars.put("submittedDate", request.getCreatedDate() != null
                    ? request.getCreatedDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")) : "");
            vars.put("approveUrl", approveUrl);
            vars.put("rejectUrl", rejectUrl);

            var rendered = emailTemplateRenderingService.render("APPROVER.REMINDER", vars);
            emailService.sendHtmlEmail(approverEmail, rendered.subject(), rendered.html(), rendered.plainText());
        }
    }
}
