package com.example.multimedia.file_upload_api.service.supplier;

import com.example.multimedia.file_upload_api.entity.CertificateOfIncorporation;
import com.example.multimedia.file_upload_api.entity.ChequeDetails;
import com.example.multimedia.file_upload_api.entity.CompanyDetails;
import com.example.multimedia.file_upload_api.entity.UserAuthentication;
import com.example.multimedia.file_upload_api.entity.UserDetail;
import com.example.multimedia.file_upload_api.entity.supplier.SupplierCompanyProfile;
import com.example.multimedia.file_upload_api.entity.supplier.SupplierRegistration;
import com.example.multimedia.file_upload_api.entity.supplier.SupplierRegistrationStatus;
import com.example.multimedia.file_upload_api.entity.workflow.WorkflowRequestStatus;
import com.example.multimedia.file_upload_api.repository.CertificateOfIncorporationRepository;
import com.example.multimedia.file_upload_api.repository.ChequeDetailsRepository;
import com.example.multimedia.file_upload_api.repository.CompanyDetailsRepository;
import com.example.multimedia.file_upload_api.repository.UserAuthenticationRepository;
import com.example.multimedia.file_upload_api.repository.UserDetailRepository;
import com.example.multimedia.file_upload_api.entity.supplier.SupplierChangeRequest;
import com.example.multimedia.file_upload_api.repository.supplier.SupplierChangeRequestRepository;
import com.example.multimedia.file_upload_api.repository.supplier.SupplierCompanyProfileRepository;
import com.example.multimedia.file_upload_api.repository.supplier.SupplierRegistrationRepository;
import com.example.multimedia.file_upload_api.service.EmailService;
import com.example.multimedia.file_upload_api.service.email.EmailTemplateRenderingService;
import com.example.multimedia.file_upload_api.service.workflow.WorkflowRequestResolvedEvent;
import com.example.multimedia.file_upload_api.util.PasswordGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Reacts to a supplier registration's workflow request resolving - provisions the real
 * login (UserDetail/UserAuthentication/CompanyDetails/SupplierCompanyProfile) on approval,
 * or just flips the registration's status on rejection/escalation. Listens for the generic
 * {@link WorkflowRequestResolvedEvent} rather than any vendor-specific callback - the
 * workflow engine itself has no idea this is a vendor at all.
 *
 * {@code AFTER_COMMIT} so provisioning only runs once the resolving transaction (the approval
 * action itself) has durably committed - never speculatively inside it.
 */
@Service
public class SupplierOnboardingProvisioningService {

    private static final Logger logger = LoggerFactory.getLogger(SupplierOnboardingProvisioningService.class);

    private final SupplierRegistrationRepository registrationRepository;
    private final UserDetailRepository userDetailRepository;
    private final UserAuthenticationRepository userAuthenticationRepository;
    private final CompanyDetailsRepository companyDetailsRepository;
    private final ChequeDetailsRepository chequeDetailsRepository;
    private final CertificateOfIncorporationRepository certificateOfIncorporationRepository;
    private final SupplierCompanyProfileRepository supplierCompanyProfileRepository;
    private final SupplierChangeRequestRepository changeRequestRepository;
    private final SupplierChangeRequestService changeRequestService;
    private final PasswordEncoder passwordEncoder;
    private final EmailTemplateRenderingService emailTemplateRenderingService;
    private final EmailService emailService;

    public SupplierOnboardingProvisioningService(SupplierRegistrationRepository registrationRepository,
                                                  UserDetailRepository userDetailRepository,
                                                  UserAuthenticationRepository userAuthenticationRepository,
                                                  CompanyDetailsRepository companyDetailsRepository,
                                                  ChequeDetailsRepository chequeDetailsRepository,
                                                  CertificateOfIncorporationRepository certificateOfIncorporationRepository,
                                                  SupplierCompanyProfileRepository supplierCompanyProfileRepository,
                                                  SupplierChangeRequestRepository changeRequestRepository,
                                                  SupplierChangeRequestService changeRequestService,
                                                  PasswordEncoder passwordEncoder,
                                                  EmailTemplateRenderingService emailTemplateRenderingService,
                                                  EmailService emailService) {
        this.registrationRepository = registrationRepository;
        this.userDetailRepository = userDetailRepository;
        this.userAuthenticationRepository = userAuthenticationRepository;
        this.companyDetailsRepository = companyDetailsRepository;
        this.chequeDetailsRepository = chequeDetailsRepository;
        this.certificateOfIncorporationRepository = certificateOfIncorporationRepository;
        this.supplierCompanyProfileRepository = supplierCompanyProfileRepository;
        this.changeRequestRepository = changeRequestRepository;
        this.changeRequestService = changeRequestService;
        this.passwordEncoder = passwordEncoder;
        this.emailTemplateRenderingService = emailTemplateRenderingService;
        this.emailService = emailService;
    }

    // REQUIRES_NEW, not just on handleRegistrationResolution below: AFTER_COMMIT fires once the
    // original transaction has already ended, so there is nothing left to join - Spring actually
    // rejects a plain @Transactional here outright. handleRegistrationResolution is then called
    // as a plain `this.` self-invocation, which bypasses the proxy entirely and silently ignores
    // its own @Transactional, so the (new) transaction has to actually start here, one level up.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onWorkflowRequestResolved(WorkflowRequestResolvedEvent event) {
        registrationRepository.findByWorkflowRequest_Id(event.getWorkflowRequestId())
                .ifPresentOrElse(
                        reg -> handleRegistrationResolution(reg.getId(), event.getStatus()),
                        () -> changeRequestRepository.findByWorkflowRequest_Id(event.getWorkflowRequestId())
                                .ifPresent(cr -> handleChangeRequestResolution(cr, event.getStatus())));
    }

    private void handleChangeRequestResolution(SupplierChangeRequest cr, WorkflowRequestStatus status) {
        if (status == WorkflowRequestStatus.APPROVED) {
            changeRequestService.applyApprovedChange(cr);
        } else if (status == WorkflowRequestStatus.REJECTED || status == WorkflowRequestStatus.ESCALATED
                || status == WorkflowRequestStatus.CANCELLED) {
            changeRequestService.rejectChange(cr);
        }
    }

    @Transactional
    public void handleRegistrationResolution(Long registrationId, WorkflowRequestStatus status) {
        SupplierRegistration reg = registrationRepository.findById(registrationId).orElse(null);
        if (reg == null) return;

        switch (status) {
            case APPROVED -> provisionVendorAccount(reg);
            case REJECTED -> {
                reg.setStatus(SupplierRegistrationStatus.REJECTED);
                registrationRepository.save(reg);
                sendRejectedEmail(reg);
            }
            case ESCALATED -> {
                reg.setStatus(SupplierRegistrationStatus.ESCALATED);
                registrationRepository.save(reg);
            }
            case CANCELLED -> {
                reg.setStatus(SupplierRegistrationStatus.CANCELLED);
                registrationRepository.save(reg);
            }
            default -> { /* PENDING - nothing to do yet */ }
        }
    }

    private void provisionVendorAccount(SupplierRegistration reg) {
        if (registrationRepository.claimProvisioning(reg.getId()) == 0) {
            logger.info("Registration {} already provisioned - ignoring duplicate approval resolution", reg.getId());
            return;
        }
        reg = registrationRepository.findById(reg.getId()).orElseThrow();

        String rawPassword = PasswordGenerator.generatePassword(12);
        String vendorCode = "VEND-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String vendorName = reg.getVendorName() != null && !reg.getVendorName().isBlank()
                ? reg.getVendorName() : reg.getBeneficiaryName();

        UserDetail user = new UserDetail();
        user.setSuperAdmin(reg.getSuperAdmin());
        user.setEmail(reg.getEmail());
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setFirstName(reg.getContactName());
        user.setPhoneNumber(reg.getPhone());
        user.setIsActive(true);
        user = userDetailRepository.save(user);

        UserAuthentication auth = new UserAuthentication();
        auth.setUserId(user.getUserId());
        auth.setAuthKey("vendor");
        auth.setIsActive(true);
        userAuthenticationRepository.save(auth);

        CompanyDetails company = new CompanyDetails();
        company.setCompanyName(vendorName);
        company.setLegalTradeName(vendorName);
        company.setRegisteredAddress(reg.getAddress());
        company.setGstinNumber(reg.getGstNumber());
        company.setPanNumber(reg.getPanNumber());
        company.setTypeOfRegistration(reg.getCompanyType());
        company.setSuperAdmin(reg.getSuperAdmin());
        company.setUser(user);
        company.setAuthKey("vendor");
        company = companyDetailsRepository.save(company);

        if (reg.getAccountNumber() != null || reg.getIfscCode() != null || reg.getBankName() != null) {
            ChequeDetails cheque = new ChequeDetails();
            cheque.setCompany(company);
            cheque.setAccountNumber(reg.getAccountNumber());
            cheque.setIfsc(reg.getIfscCode());
            cheque.setBank(reg.getBankName());
            cheque.setIssuedTo(reg.getBeneficiaryName());
            chequeDetailsRepository.save(cheque);
        }

        if (reg.getCinNumber() != null) {
            CertificateOfIncorporation coi = new CertificateOfIncorporation();
            coi.setCompany(company);
            coi.setCinNumber(reg.getCinNumber());
            certificateOfIncorporationRepository.save(coi);
        }

        SupplierCompanyProfile profile = new SupplierCompanyProfile();
        profile.setCompany(company);
        profile.setRegistration(reg);
        profile.setVendorType(reg.getVendorType());
        profile.setContactName(reg.getContactName());
        profile.setDesignation(reg.getDesignation());
        profile.setEmail(reg.getEmail());
        profile.setPhone(reg.getPhone());
        profile.setContact1Name(reg.getContact1Name());
        profile.setContact1Role(reg.getContact1Role());
        profile.setContact1Email(reg.getContact1Email());
        profile.setContact1Phone(reg.getContact1Phone());
        profile.setContact2Name(reg.getContact2Name());
        profile.setContact2Role(reg.getContact2Role());
        profile.setContact2Email(reg.getContact2Email());
        profile.setContact2Phone(reg.getContact2Phone());
        profile.setPrimaryContact(reg.getPrimaryContact());
        profile.setBusinessTypes(reg.getBusinessTypes());
        profile.setBusinessScope(reg.getBusinessScope());
        profile.setCompanyType(reg.getCompanyType());
        profile.setMsmeNumber(reg.getMsmeNumber());
        profile.setCinNumber(reg.getCinNumber());
        profile.setForeignTaxId(reg.getForeignTaxId());
        profile.setForeignRegNo(reg.getForeignRegNo());
        profile.setForeignAcctNo(reg.getForeignAcctNo());
        profile.setSwiftCode(reg.getSwiftCode());
        profile.setIsoCertificateNo(reg.getIsoCertificateNo());
        profile.setIsoCertifyingBody(reg.getIsoCertifyingBody());
        profile.setIsoExpiry(reg.getIsoExpiry());
        profile.setIso14001CertificateNo(reg.getIso14001CertificateNo());
        profile.setIso14001CertifyingBody(reg.getIso14001CertifyingBody());
        profile.setIso14001Expiry(reg.getIso14001Expiry());
        profile.setIso45001CertificateNo(reg.getIso45001CertificateNo());
        profile.setIso45001CertifyingBody(reg.getIso45001CertifyingBody());
        profile.setIso45001Expiry(reg.getIso45001Expiry());
        profile.setIso27001CertificateNo(reg.getIso27001CertificateNo());
        profile.setIso27001CertifyingBody(reg.getIso27001CertifyingBody());
        profile.setIso27001Expiry(reg.getIso27001Expiry());
        profile.setAs9100dCertificateNo(reg.getAs9100dCertificateNo());
        profile.setAs9100dCertifyingBody(reg.getAs9100dCertifyingBody());
        profile.setAs9100dExpiry(reg.getAs9100dExpiry());
        profile.setNadcapCertificateNo(reg.getNadcapCertificateNo());
        profile.setNadcapExpiry(reg.getNadcapExpiry());
        profile.setVerificationNotes(reg.getVerificationNotes());
        supplierCompanyProfileRepository.save(profile);

        reg.setVendorName(vendorName);
        reg.setStatus(SupplierRegistrationStatus.ACTIVE);
        reg.setVendorCode(vendorCode);
        reg.setUserId(user.getUserId());
        reg.setCompanyId(company.getCompanyId());
        reg.setApprovedDate(LocalDateTime.now());
        registrationRepository.save(reg);

        sendApprovedEmail(reg, rawPassword);
    }

    private void sendApprovedEmail(SupplierRegistration reg, String rawPassword) {
        try {
            Map<String, Object> vars = new HashMap<>();
            vars.put("contactName", reg.getContactName() != null ? reg.getContactName() : "there");
            vars.put("vendorName", reg.getVendorName());
            vars.put("vendorCode", reg.getVendorCode());
            vars.put("loginEmail", reg.getEmail());
            vars.put("tempPassword", rawPassword);
            var rendered = emailTemplateRenderingService.render("SUPPLIER.APPROVED", vars);
            emailService.sendHtmlEmail(reg.getEmail(), rendered.subject(), rendered.html(), rendered.plainText());
        } catch (Exception e) {
            logger.warn("Could not send approval email for registration {}", reg.getId(), e);
        }
    }

    private void sendRejectedEmail(SupplierRegistration reg) {
        try {
            Map<String, Object> vars = new HashMap<>();
            vars.put("contactName", reg.getContactName() != null ? reg.getContactName() : "there");
            vars.put("vendorName", reg.getVendorName() != null ? reg.getVendorName() : "your application");
            vars.put("rejectionReason", reg.getVerificationNotes() != null ? reg.getVerificationNotes() : "See reviewer comments.");
            var rendered = emailTemplateRenderingService.render("SUPPLIER.REJECTED", vars);
            emailService.sendHtmlEmail(reg.getEmail(), rendered.subject(), rendered.html(), rendered.plainText());
        } catch (Exception e) {
            logger.warn("Could not send rejection email for registration {}", reg.getId(), e);
        }
    }
}
