package com.example.multimedia.file_upload_api.entity.supplier;

import com.example.multimedia.file_upload_api.entity.SuperAdmin;
import com.example.multimedia.file_upload_api.entity.workflow.WorkflowRequest;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * One row per "Become a Supplier" application, self-contained to MDG- (no FolderIt, no
 * WorkFlow service, no multi-company routing - single generic approval chain for v1). Field
 * shape is a trimmed port of backend_java's SupplierRegistration: the multi-company
 * classification/TDS/routing fields and the Form Studio dynamic-questionnaire fields are
 * dropped since there is nothing here for them to route against or be answered by.
 */
@Entity
@Table(name = "supplier_registration")
@Getter
@Setter
public class SupplierRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Derived from the cancelled cheque's verified account-holder name at submit time if blank -
    // there is no separate "company name" field the applicant types.
    @Column(name = "vendor_name")
    private String vendorName;

    private String address;

    /** DOMESTIC / FOREIGN - decides which of SupplierDocumentConfig's two document lists applies. */
    @Column(name = "vendor_type", nullable = false)
    private String vendorType = "DOMESTIC";

    @ManyToOne
    @JoinColumn(name = "super_admin_id", nullable = false)
    private SuperAdmin superAdmin;

    // ── Resolved primary contact ──────────────────────────────────────────
    @Column(name = "contact_name")
    private String contactName;

    private String designation;

    @Email
    @Column(unique = true)
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

    /** 1 or 2 - which of the above is the primary contact. */
    @Column(name = "primary_contact")
    private Integer primaryContact = 1;

    // ── Business profile ──────────────────────────────────────────────────
    @Column(name = "business_types", columnDefinition = "TEXT")
    private String businessTypes;

    @Column(name = "business_scope", columnDefinition = "TEXT")
    private String businessScope;

    /** Proprietary / Partnership / Private Limited / Public Limited. */
    @Column(name = "company_type")
    private String companyType;

    private String telephone;
    private String fax;

    @Column(name = "weekly_off")
    private String weeklyOff;

    @Column(name = "annual_turnover")
    private String annualTurnover;

    @Column(name = "turnover_year")
    private String turnoverYear;

    @Column(name = "regulatory_acts", columnDefinition = "TEXT")
    private String regulatoryActs;

    @Column(name = "manpower_office")
    private String manpowerOffice;

    @Column(name = "manpower_supervisor")
    private String manpowerSupervisor;

    @Column(name = "manpower_workmen")
    private String manpowerWorkmen;

    @Column(name = "shifts_per_day")
    private String shiftsPerDay;

    @Column(name = "spare_capacity")
    private String spareCapacity;

    @Column(name = "floor_space")
    private String floorSpace;

    @Column(name = "equipment_facilities", columnDefinition = "TEXT")
    private String equipmentFacilities;

    /** JSON array of {name, qualification, experience, commencementDate, capitalEmployed}. */
    @Column(name = "directors_json", columnDefinition = "LONGTEXT")
    private String directorsJson;

    /** JSON array of {description, capacity, makeName, makeYear, numbers, remarks}. */
    @Column(name = "machinery_json", columnDefinition = "LONGTEXT")
    private String machineryJson;

    /** Code-of-conduct / no-child-labour declaration checkbox. */
    @Column(name = "declaration_accepted")
    private Boolean declarationAccepted = false;

    /** DPDP Act, 2023 consent to collect/store/process personal data - independent legal basis
     *  from declarationAccepted above, persisted (unlike backend_java's @Transient field) so
     *  there is an actual record of consent if it's ever disputed. */
    @Column(name = "dpdp_consent_accepted")
    private Boolean dpdpConsentAccepted = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SupplierRegistrationStatus status = SupplierRegistrationStatus.DRAFT;

    @Column(name = "approved_date")
    private LocalDateTime approvedDate;

    /** Unique short code emailed on "Save Draft", used to resume later without an account. */
    @Column(name = "resume_code", unique = true)
    private String resumeCode;

    @ManyToOne
    @JoinColumn(name = "workflow_request_id")
    private WorkflowRequest workflowRequest;

    /** Generated on approval. */
    @Column(name = "vendor_code", unique = true)
    private String vendorCode;

    /** Set once the real login (UserDetail/UserAuthentication/CompanyDetails) is provisioned. */
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "company_id")
    private Long companyId;

    // ── KYC ──────────────────────────────────────────────────────────────
    @Column(name = "gst_number")
    private String gstNumber;

    @Column(name = "pan_number")
    private String panNumber;

    @Column(name = "is_msme")
    private Boolean isMsme;

    @Column(name = "msme_number")
    private String msmeNumber;

    @Column(name = "cin_number")
    private String cinNumber;

    // ── Foreign vendor equivalents (reviewed by hand, no live verification) ─
    @Column(name = "foreign_tax_id")
    private String foreignTaxId;

    @Column(name = "foreign_reg_no")
    private String foreignRegNo;

    // ── Bank ─────────────────────────────────────────────────────────────
    @Column(name = "beneficiary_name")
    private String beneficiaryName;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "ifsc_code")
    private String ifscCode;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "foreign_acct_no")
    private String foreignAcctNo;

    @Column(name = "swift_code")
    private String swiftCode;

    // ── Optional certifications ─────────────────────────────────────────
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
