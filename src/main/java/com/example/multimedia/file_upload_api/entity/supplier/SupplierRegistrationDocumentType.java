package com.example.multimedia.file_upload_api.entity.supplier;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * A document type an approver has granted a vendor (e.g. "Products"), scoped to a single
 * generic company in v1 - unlike backend_java's version, there is no per-company-code matrix
 * here, since MDG- doesn't model multiple SAP company codes.
 */
@Data
@NoArgsConstructor
@Entity
@IdClass(SupplierRegistrationDocumentType.Pk.class)
@Table(name = "supplier_registration_document_type")
public class SupplierRegistrationDocumentType {

    @Id
    @Column(name = "registration_id")
    private Long registrationId;

    @Id
    @Column(name = "doc_type_code", length = 20)
    private String docTypeCode;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private LocalDateTime createdAt;

    /** TDS section(s) picked when this is a Service document type, comma-joined (e.g. "194C,194J"). */
    @Column(name = "withholding_tax_key", length = 60)
    private String withholdingTaxKey;

    public SupplierRegistrationDocumentType(Long registrationId, String docTypeCode) {
        this.registrationId = registrationId;
        this.docTypeCode = docTypeCode;
    }

    @Data
    @NoArgsConstructor
    public static class Pk implements Serializable {
        private Long registrationId;
        private String docTypeCode;
    }
}
