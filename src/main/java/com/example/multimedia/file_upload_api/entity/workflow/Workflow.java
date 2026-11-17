package com.example.multimedia.file_upload_api.entity.workflow;

import com.example.multimedia.file_upload_api.entity.SuperAdmin;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A reusable, generic multi-stage approval definition - not vendor-specific, so other MDG-
 * features can reuse this engine later. Editing a workflow's stages never retroactively
 * changes an in-flight {@link WorkflowRequest}, since every request freezes its own snapshot
 * at creation time (see {@link WorkflowRequest#getWorkflowSnapshotJson()}).
 */
@Entity
@Table(name = "workflow")
@Getter
@Setter
public class Workflow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Stable lookup key, e.g. "VENDOR_ONBOARDING" / "VENDOR_CHANGE_REQUEST". Named workflowKey,
     *  not key - "KEY" is a reserved word in MySQL and best avoided as a bare column name. */
    @Column(name = "workflow_key", nullable = false, unique = true, length = 60)
    private String workflowKey;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne
    @JoinColumn(name = "super_admin_id", nullable = false)
    private SuperAdmin superAdmin;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("stageOrder ASC")
    private List<WorkflowStage> stages = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;
}
