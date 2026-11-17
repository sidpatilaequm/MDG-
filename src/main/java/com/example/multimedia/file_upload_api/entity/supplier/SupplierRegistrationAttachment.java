package com.example.multimedia.file_upload_api.entity.supplier;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * A free-form extra file attached to a {@link SupplierRegistration} - no fixed docType slot, no
 * OCR/verification. The bytes live in {@link SupplierRegistrationAttachmentBlob}.
 */
@Entity
@Table(name = "supplier_registration_attachment")
@Getter
@Setter
public class SupplierRegistrationAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "registration_id", nullable = false)
    private SupplierRegistration registration;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "content_type")
    private String contentType;

    @CreationTimestamp
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;
}
