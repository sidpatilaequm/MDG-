package com.example.multimedia.file_upload_api.controller.supplier;

import com.example.multimedia.file_upload_api.entity.workflow.ApproverGroup;
import com.example.multimedia.file_upload_api.entity.workflow.ApproverGroupMember;
import com.example.multimedia.file_upload_api.service.workflow.ApproverGroupAdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Lets a super_admin manage who sits in each approver group (e.g. "Default Approvers") -
 *  without at least one active member, a workflow stage can never be decided and
 *  StageAdvancementService skips it automatically (see its activateNextEligibleStages). */
@RestController
@RequestMapping("/api/admin/approver-groups")
public class ApproverGroupController {

    private final ApproverGroupAdminService approverGroupAdminService;

    public ApproverGroupController(ApproverGroupAdminService approverGroupAdminService) {
        this.approverGroupAdminService = approverGroupAdminService;
    }

    @GetMapping
    public ResponseEntity<List<ApproverGroup>> list() {
        return ResponseEntity.ok(approverGroupAdminService.listGroups());
    }

    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<ApproverGroupMember>> listMembers(@PathVariable Long groupId) {
        return ResponseEntity.ok(approverGroupAdminService.listMembers(groupId));
    }

    @PostMapping("/{groupId}/members")
    public ResponseEntity<ApproverGroupMember> addMember(@PathVariable Long groupId, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(approverGroupAdminService.addMember(groupId, body.get("email")));
    }

    @DeleteMapping("/{groupId}/members/{memberId}")
    public ResponseEntity<Void> removeMember(@PathVariable Long groupId, @PathVariable Long memberId) {
        approverGroupAdminService.removeMember(groupId, memberId);
        return ResponseEntity.noContent().build();
    }
}
