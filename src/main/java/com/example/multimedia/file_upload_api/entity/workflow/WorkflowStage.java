package com.example.multimedia.file_upload_api.entity.workflow;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "workflow_stage")
@Getter
@Setter
public class WorkflowStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @Column(name = "stage_order", nullable = false)
    private Integer stageOrder;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "approver_group_id", nullable = false)
    private ApproverGroup approverGroup;

    @Column(name = "sla_hours")
    private Integer slaHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "voting_rule", nullable = false, length = 20)
    private VotingRule votingRule = VotingRule.ANY;

    @Enumerated(EnumType.STRING)
    @Column(name = "rejection_behavior", nullable = false, length = 20)
    private RejectionBehavior rejectionBehavior = RejectionBehavior.STOP;

    @Column(name = "is_optional", nullable = false)
    private Boolean isOptional = false;

    /** Evaluated against WorkflowRequest.requestMetadataJson - null means "always run". */
    @Column(name = "condition_field")
    private String conditionField;

    @Column(name = "condition_op", length = 20)
    private String conditionOp;

    @Column(name = "condition_value")
    private String conditionValue;

    /** Stages sharing the same non-null value run concurrently and must all resolve before the
     *  request advances past the group - unused in v1's single linear chain, kept for later. */
    @Column(name = "parallel_group")
    private Integer parallelGroup;

    /** When true, this stage can only be decided through the authenticated portal - the
     *  one-click email-link APPROVE is blocked (REJECT still works via the link). */
    @Column(name = "requires_portal_action", nullable = false)
    private Boolean requiresPortalAction = false;
}
