package com.example.multimedia.file_upload_api.entity.supplier;

import com.example.multimedia.file_upload_api.entity.CompanyDetails;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Rich, onboarding-specific profile data for an approved vendor - everything MDG-'s existing,
 * narrow {@link CompanyDetails} (shared with the materials-catalog business) has no column for:
 * contacts, business profile, certifications, MSME/UDYAM, foreign-vendor fields, verification
 * notes. One row per approved {@link SupplierRegistration}.
 */
@Entity
@Table(name = "supplier_company_profile")
@Getter
@Setter
public class SupplierCompanyProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "company_id", nullable = false, unique = true)
    private CompanyDetails company;

    @OneToOne
    @JoinColumn(name = "registration_id", nullable = false, unique = true)
    private SupplierRegistration registration;

    private String vendorType;

    @Column(name = "contact_name")
    private String contactName;
    private String designation;
    private String email;
    private String phone;

    @Column(name = "contact1_name")
    private String contact1Name;
    @Column(name = "contact1_role")
    private String contact1Role;
    @Column(name = "contact1_email")
    private String contact1Email;
    @Column(name = "contact1_phone")
    private String contact1Phone;

    @Column(name = "contact2_name")
    private String contact2Name;
    @Column(name = "contact2_role")
    private String contact2Role;
    @Column(name = "contact2_email")
    private String contact2Email;
    @Column(name = "contact2_phone")
    private String contact2Phone;

    @Column(name = "primary_contact")
    private Integer primaryContact;

    @Column(name = "business_types", columnDefinition = "TEXT")
    private String businessTypes;
    @Column(name = "business_scope", columnDefinition = "TEXT")
    private String businessScope;
    @Column(name = "company_type")
    private String companyType;

    @Column(name = "msme_number")
    private String msmeNumber;
    @Column(name = "cin_number")
    private String cinNumber;

    @Column(name = "foreign_tax_id")
    private String foreignTaxId;
    @Column(name = "foreign_reg_no")
    private String foreignRegNo;
    @Column(name = "foreign_acct_no")
    private String foreignAcctNo;
    @Column(name = "swift_code")
    private String swiftCode;

    @Column(name = "iso_certificate_no")
    private String isoCertificateNo;
    @Column(name = "iso_certifying_body")
    private String isoCertifyingBody;
    @Column(name = "iso_expiry")
    private String isoExpiry;

    @Column(name = "iso14001_certificate_no")
    private String iso14001CertificateNo;
    @Column(name = "iso14001_certifying_body")
    private String iso14001CertifyingBody;
    @Column(name = "iso14001_expiry")
    private String iso14001Expiry;

    @Column(name = "iso45001_certificate_no")
    private String iso45001CertificateNo;
    @Column(name = "iso45001_certifying_body")
    private String iso45001CertifyingBody;
    @Column(name = "iso45001_expiry")
    private String iso45001Expiry;

    @Column(name = "iso27001_certificate_no")
    private String iso27001CertificateNo;
    @Column(name = "iso27001_certifying_body")
    private String iso27001CertifyingBody;
    @Column(name = "iso27001_expiry")
    private String iso27001Expiry;

    @Column(name = "as9100d_certificate_no")
    private String as9100dCertificateNo;
    @Column(name = "as9100d_certifying_body")
    private String as9100dCertifyingBody;
    @Column(name = "as9100d_expiry")
    private String as9100dExpiry;

    @Column(name = "nadcap_certificate_no")
    private String nadcapCertificateNo;
    @Column(name = "nadcap_expiry")
    private String nadcapExpiry;

    @Column(name = "verification_notes", columnDefinition = "TEXT")
    private String verificationNotes;

    @CreationTimestamp
    @Column(name = "created_date", updatable = false)
    private LocalDateTime createdDate;

    @UpdateTimestamp
    @Column(name = "modified_date")
    private LocalDateTime modifiedDate;
}
