package com.example.multimedia.file_upload_api.repository.workflow;

import com.example.multimedia.file_upload_api.entity.workflow.ApproverGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApproverGroupRepository extends JpaRepository<ApproverGroup, Long> {
}
