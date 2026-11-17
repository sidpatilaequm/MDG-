package com.example.multimedia.file_upload_api.entity.workflow;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * One in-flight (or resolved) run of a {@link Workflow}. {@code submitterId} is nullable with
 * no hard FK, since the public supplier-registration flow has no logged-in user yet at submit
 * time - applicant identity instead lives in {@code requestMetadataJson}.
 *
 * {@code workflowSnapshotJson} is the critical isolation mechanism: a frozen copy of this
 * request's stages + approver-group members, captured once at creation. Every later
 * advance/condition/email decision reads this snapshot, never the live {@link Workflow}/
 * {@link WorkflowStage}/{@link ApproverGroup} tables - so an admin editing the workflow
 * definition never retroactively changes a request that's already in flight.
 */
@Entity
@Table(name = "workflow_request")
@Getter
@Setter
public class WorkflowRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @Column(name = "submitter_id")
    private Long submitterId;

    @Column(nullable = false)
    private String title;

    @Column(name = "request_metadata_json", columnDefinition = "LONGTEXT")
    private String requestMetadataJson;

    @Column(name = "workflow_snapshot_json", columnDefinition = "LONGTEXT", nullable = false)
    private String workflowSnapshotJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WorkflowRequestStatus status = WorkflowRequestStatus.PENDING;

    @Column(name = "current_stage_order")
    private Integer currentStageOrder;

    @OneToMany(mappedBy = "workflowRequest", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stageOrder ASC")
    private List<RequestStage> stages = new ArrayList<>();

    @Column(name = "resolved_date")
    private LocalDateTime resolvedDate;

    @CreationTimestamp
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;
}
