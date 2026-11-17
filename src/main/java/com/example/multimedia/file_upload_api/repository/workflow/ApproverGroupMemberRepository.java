package com.example.multimedia.file_upload_api.repository.workflow;

import com.example.multimedia.file_upload_api.entity.workflow.ApproverGroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApproverGroupMemberRepository extends JpaRepository<ApproverGroupMember, Long> {

    List<ApproverGroupMember> findByApproverGroup_IdAndIsActiveTrue(Long approverGroupId);

    boolean existsByApproverGroup_IdAndUserIdAndIsActiveTrue(Long approverGroupId, Long userId);
}
