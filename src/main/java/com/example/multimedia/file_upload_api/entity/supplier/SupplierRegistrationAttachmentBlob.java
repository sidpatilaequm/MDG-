package com.example.multimedia.file_upload_api.entity.supplier;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "supplier_registration_attachment_blob")
@Getter
@Setter
public class SupplierRegistrationAttachmentBlob {

    @Id
    private Long attachmentId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "attachment_id")
    private SupplierRegistrationAttachment attachment;

    @Lob
    @Column(name = "file_data", columnDefinition = "LONGBLOB")
    private byte[] fileData;
}
