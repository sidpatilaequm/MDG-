package com.example.multimedia.file_upload_api.controller.supplier;

import com.example.multimedia.file_upload_api.entity.workflow.ApprovalDecision;
import com.example.multimedia.file_upload_api.service.CurrentUserService;
import com.example.multimedia.file_upload_api.service.workflow.ApprovalActionTokenService;
import com.example.multimedia.file_upload_api.service.workflow.StageAdvancementService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Approval decisions - both the authenticated-portal path ({@code /decide}, matches
 * SecurityConfig's {@code /api/approvals/**} rule: approver or super_admin) and the one-click
 * email-link path ({@code /action/{token}}, permitAll at the Spring Security layer - this
 * controller validates the action-token JWT itself before doing anything, and a stage with
 * {@code requiresPortalAction} rejects an APPROVE attempted this way).
 */
@RestController
@RequestMapping("/api/approvals")
public class ApprovalController {

    private final StageAdvancementService stageAdvancementService;
    private final ApprovalActionTokenService actionTokenService;
    private final CurrentUserService currentUserService;

    public ApprovalController(StageAdvancementService stageAdvancementService,
                               ApprovalActionTokenService actionTokenService,
                               CurrentUserService currentUserService) {
        this.stageAdvancementService = stageAdvancementService;
        this.actionTokenService = actionTokenService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/{requestStageId}/decide")
    public ResponseEntity<?> decide(@PathVariable Long requestStageId, @RequestBody Map<String, String> body) {
        var principal = currentUserService.getCurrentPrincipalIdentity();
        ApprovalDecision decision = ApprovalDecision.valueOf(body.get("decision").toUpperCase());
        try {
            stageAdvancementService.recordAction(requestStageId, principal.id(), principal.email(),
                    decision, body.get("comment"), false);
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    @GetMapping(value = "/action/{token}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> decideViaEmailLink(@PathVariable String token) {
        try {
            ApprovalActionTokenService.ActionTokenPayload payload = actionTokenService.parse(token);
            stageAdvancementService.recordAction(payload.requestStageId(), payload.approverId(),
                    payload.approverEmail(), payload.action(), "Decided via one-click email link", true);
            return ResponseEntity.ok(htmlPage("Thank you",
                    "Your decision (" + payload.action() + ") has been recorded."));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(htmlPage("Could not record decision", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(htmlPage("Link no longer valid",
                    "This approval link has expired or is invalid. Please log in to the portal instead."));
        }
    }

    private String htmlPage(String title, String message) {
        return "<!doctype html><html><head><meta charset=\"utf-8\"><title>" + title + "</title></head>"
                + "<body style=\"font-family:sans-serif;max-width:480px;margin:60px auto;text-align:center;\">"
                + "<h2>" + title + "</h2><p>" + message + "</p></body></html>";
    }
}
