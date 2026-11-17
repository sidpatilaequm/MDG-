package com.example.multimedia.file_upload_api.service.workflow;

import com.example.multimedia.file_upload_api.entity.SuperAdmin;
import com.example.multimedia.file_upload_api.entity.UserDetail;
import com.example.multimedia.file_upload_api.entity.workflow.ApproverGroup;
import com.example.multimedia.file_upload_api.entity.workflow.ApproverGroupMember;
import com.example.multimedia.file_upload_api.repository.SuperAdminRepository;
import com.example.multimedia.file_upload_api.repository.UserDetailRepository;
import com.example.multimedia.file_upload_api.repository.workflow.ApproverGroupMemberRepository;
import com.example.multimedia.file_upload_api.repository.workflow.ApproverGroupRepository;
import com.example.multimedia.file_upload_api.service.CurrentUserService;
import org.springframework.stereotype.Service;

import java.util.List;

/** Minimal admin-facing management of approver groups/members - someone has to be able to add
 *  the people who will actually approve supplier registrations. A fuller admin UI for
 *  creating/editing workflows themselves is out of scope for v1 (one generic workflow is
 *  seeded by V8's migration); this only manages group membership. */
@Service
public class ApproverGroupAdminService {

    private final ApproverGroupRepository approverGroupRepository;
    private final ApproverGroupMemberRepository approverGroupMemberRepository;
    private final UserDetailRepository userDetailRepository;
    private final SuperAdminRepository superAdminRepository;
    private final CurrentUserService currentUserService;

    public ApproverGroupAdminService(ApproverGroupRepository approverGroupRepository,
                                      ApproverGroupMemberRepository approverGroupMemberRepository,
                                      UserDetailRepository userDetailRepository,
                                      SuperAdminRepository superAdminRepository,
                                      CurrentUserService currentUserService) {
        this.approverGroupRepository = approverGroupRepository;
        this.approverGroupMemberRepository = approverGroupMemberRepository;
        this.userDetailRepository = userDetailRepository;
        this.superAdminRepository = superAdminRepository;
        this.currentUserService = currentUserService;
    }

    public List<ApproverGroup> listGroups() {
        Long superAdminId = currentUserService.getCurrentSuperAdminId();
        return approverGroupRepository.findAll().stream()
                .filter(g -> g.getSuperAdmin().getSuperAdminId().equals(superAdminId))
                .toList();
    }

    public List<ApproverGroupMember> listMembers(Long groupId) {
        requireOwnedGroup(groupId);
        return approverGroupMemberRepository.findByApproverGroup_IdAndIsActiveTrue(groupId);
    }

    public ApproverGroupMember addMember(Long groupId, String email) {
        ApproverGroup group = requireOwnedGroup(groupId);

        Long userId;
        Long superAdminId = currentUserService.getCurrentSuperAdminId();
        var userOpt = userDetailRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            userId = userOpt.get().getUserId();
        } else {
            SuperAdmin superAdmin = superAdminRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("No user or admin account found for email: " + email));
            userId = superAdmin.getSuperAdminId();
        }

        if (approverGroupMemberRepository.existsByApproverGroup_IdAndUserIdAndIsActiveTrue(groupId, userId)) {
            throw new IllegalStateException(email + " is already a member of this approver group");
        }

        ApproverGroupMember member = new ApproverGroupMember();
        member.setApproverGroup(group);
        member.setUserId(userId);
        member.setUserEmail(email);
        member.setIsActive(true);
        return approverGroupMemberRepository.save(member);
    }

    public void removeMember(Long groupId, Long memberId) {
        requireOwnedGroup(groupId);
        ApproverGroupMember member = approverGroupMemberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));
        if (!member.getApproverGroup().getId().equals(groupId)) {
            throw new IllegalArgumentException("Member does not belong to this group");
        }
        member.setIsActive(false);
        approverGroupMemberRepository.save(member);
    }

    private ApproverGroup requireOwnedGroup(Long groupId) {
        ApproverGroup group = approverGroupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Approver group not found"));
        if (!group.getSuperAdmin().getSuperAdminId().equals(currentUserService.getCurrentSuperAdminId())) {
            throw new IllegalArgumentException("Approver group not found");
        }
        return group;
    }
}
