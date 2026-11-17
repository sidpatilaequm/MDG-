package com.example.multimedia.file_upload_api.entity.workflow;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** One stage's live progress within a {@link WorkflowRequest}. Everything about what this stage
 *  requires (approver group members, voting rule, SLA hours) comes from the request's frozen
 *  {@code workflowSnapshotJson}, not from re-reading {@link WorkflowStage} - this row only
 *  tracks state (status, deadline, breach flag) as the request actually progresses. */
@Entity
@Table(name = "request_stage")
@Getter
@Setter
public class RequestStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "workflow_request_id", nullable = false)
    private WorkflowRequest workflowRequest;

    @Column(name = "stage_order", nullable = false)
    private Integer stageOrder;

    @Column(name = "stage_name", nullable = false)
    private String stageName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStageStatus status = RequestStageStatus.WAITING;

    @Column(name = "sla_deadline")
    private LocalDateTime slaDeadline;

    @Column(name = "is_sla_breached", nullable = false)
    private Boolean isSlaBreached = false;

    @Column(name = "reminder_sent", nullable = false)
    private Boolean reminderSent = false;

    @Column(name = "started_date")
    private LocalDateTime startedDate;

    @Column(name = "resolved_date")
    private LocalDateTime resolvedDate;

    @OneToMany(mappedBy = "requestStage", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApprovalAction> actions = new ArrayList<>();
}
