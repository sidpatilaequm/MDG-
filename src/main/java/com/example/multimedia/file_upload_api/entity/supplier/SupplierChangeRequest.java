package com.example.multimedia.file_upload_api.entity.supplier;

import com.example.multimedia.file_upload_api.entity.workflow.WorkflowRequest;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * A post-approval edit an active vendor submits for a field on their own profile (e.g. new
 * bank details) - routed through the same workflow engine as the original registration, never
 * applied directly.
 */
@Entity
@Table(name = "supplier_change_request")
@Getter
@Setter
public class SupplierChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "registration_id", nullable = false)
    private SupplierRegistration registration;

    /** The field on SupplierCompanyProfile/CompanyDetails being changed, e.g. "bankName". */
    @Column(name = "field_name", nullable = false)
    private String fieldName;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    /** PENDING / APPROVED / REJECTED */
    @Column(nullable = false, length = 20)
    private String status = "PENDING";

    @ManyToOne
    @JoinColumn(name = "workflow_request_id")
    private WorkflowRequest workflowRequest;

    @CreationTimestamp
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;
}
