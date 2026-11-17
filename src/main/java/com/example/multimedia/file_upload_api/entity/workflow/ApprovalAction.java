package com.example.multimedia.file_upload_api.entity.workflow;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "approval_action")
@Getter
@Setter
public class ApprovalAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "request_stage_id", nullable = false)
    private RequestStage requestStage;

    @Column(name = "approver_id", nullable = false)
    private Long approverId;

    @Column(name = "approver_email", nullable = false)
    private String approverEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalDecision decision;

    @Column(columnDefinition = "TEXT")
    private String comment;

    /** True when decided through the one-click email link rather than the authenticated portal. */
    @Column(name = "via_email_link", nullable = false)
    private Boolean viaEmailLink = false;

    @CreationTimestamp
    @Column(name = "acted_date", updatable = false)
    private LocalDateTime actedDate;
}
