package com.example.multimedia.file_upload_api.entity.supplier;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** The actual file bytes for a {@link SupplierRegistrationDocument}, kept in its own table so a
 *  plain document list/review query never loads them (same pattern as MDG-'s existing
 *  MaterialImage/VendorCatalogue blob tables). */
@Entity
@Table(name = "supplier_registration_document_blob")
@Getter
@Setter
public class SupplierRegistrationDocumentBlob {

    @Id
    private Long documentId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "document_id")
    private SupplierRegistrationDocument document;

    @Lob
    @Column(name = "file_data", columnDefinition = "LONGBLOB")
    private byte[] fileData;
}
