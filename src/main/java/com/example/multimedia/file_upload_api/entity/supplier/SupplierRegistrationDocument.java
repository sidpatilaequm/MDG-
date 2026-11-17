package com.example.multimedia.file_upload_api.entity.supplier;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * One row per document type per {@link SupplierRegistration}. The file bytes live in
 * {@link SupplierRegistrationDocumentBlob}, a separate 1:1 table, so listing/reviewing a
 * registration's documents never drags multi-MB blobs into memory.
 */
@Entity
@Table(name = "supplier_registration_document",
        uniqueConstraints = @UniqueConstraint(name = "uk_srd_registration_doc_type", columnNames = {"registration_id", "doc_type"}))
@Getter
@Setter
public class SupplierRegistrationDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "registration_id", nullable = false)
    private SupplierRegistration registration;

    /** coi / gst / pan / chq / udyam / iso / iso14001 / iso45001 / iso27001 / as / nadcap / nda / taxreg */
    @Column(name = "doc_type", nullable = false)
    private String docType;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "ocr_extracted_fields", columnDefinition = "LONGTEXT")
    private String ocrExtractedFieldsJson;

    /** reading / read / verifying / verified / error */
    @Column(name = "verify_status")
    private String verifyStatus;

    @Column(name = "verify_details", columnDefinition = "LONGTEXT")
    private String verifyDetailsJson;

    @CreationTimestamp
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;
}
