package com.example.multimedia.file_upload_api.entity.email;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * A branded, editable email template, keyed by a stable key (e.g. "SUPPLIER.SUBMITTED"). Used
 * for both applicant-facing and approver-notification emails through one unified rendering
 * mechanism ({@code {{placeholder}}} substitution - see EmailTemplateRenderingService).
 * Global in v1 - no per-tenant overrides.
 */
@Entity
@Table(name = "email_template")
@Getter
@Setter
public class EmailTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_key", nullable = false, unique = true, length = 80)
    private String templateKey;

    @Column(nullable = false)
    private String subject;

    @Column(name = "html_body", columnDefinition = "LONGTEXT", nullable = false)
    private String htmlBody;

    @Column(name = "plain_text_body", columnDefinition = "LONGTEXT")
    private String plainTextBody;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;
}
