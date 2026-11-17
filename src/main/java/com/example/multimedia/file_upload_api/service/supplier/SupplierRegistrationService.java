package com.example.multimedia.file_upload_api.service.supplier;

import com.example.multimedia.file_upload_api.dto.ServiceResponse;
import com.example.multimedia.file_upload_api.dto.supplier.VerifyResult;
import com.example.multimedia.file_upload_api.entity.SuperAdmin;
import com.example.multimedia.file_upload_api.entity.supplier.SupplierRegistration;
import com.example.multimedia.file_upload_api.entity.supplier.SupplierRegistrationStatus;
import com.example.multimedia.file_upload_api.repository.SuperAdminRepository;
import com.example.multimedia.file_upload_api.repository.supplier.SupplierRegistrationDocumentRepository;
import com.example.multimedia.file_upload_api.repository.supplier.SupplierRegistrationRepository;
import com.example.multimedia.file_upload_api.service.CurrentUserService;
import com.example.multimedia.file_upload_api.service.EmailService;
import com.example.multimedia.file_upload_api.service.email.EmailTemplateRenderingService;
import com.example.multimedia.file_upload_api.service.workflow.WorkflowEngineService;
import com.example.multimedia.file_upload_api.util.SupplierDocumentConfig;
import com.example.multimedia.file_upload_api.utils.AppConstants;
import com.example.multimedia.file_upload_api.utils.ServiceControllerUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

@Service
public class SupplierRegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(SupplierRegistrationService.class);

    private static final Set<SupplierRegistrationStatus> EDITABLE_STATUSES =
            Set.of(SupplierRegistrationStatus.DRAFT, SupplierRegistrationStatus.REJECTED);

    private final SupplierRegistrationRepository registrationRepository;
    private final SupplierRegistrationDocumentRepository documentRepository;
    private final SuperAdminRepository superAdminRepository;
    private final MicrovistaService microvistaService;
    private final WorkflowEngineService workflowEngineService;
    private final EmailTemplateRenderingService emailTemplateRenderingService;
    private final EmailService emailService;
    private final ServiceControllerUtils serviceControllerUtils;
    private final CurrentUserService currentUserService;
    private final String owningAdminEmail;

    public SupplierRegistrationService(SupplierRegistrationRepository registrationRepository,
                                        SupplierRegistrationDocumentRepository documentRepository,
                                        SuperAdminRepository superAdminRepository,
                                        MicrovistaService microvistaService,
                                        WorkflowEngineService workflowEngineService,
                                        EmailTemplateRenderingService emailTemplateRenderingService,
                                        EmailService emailService,
                                        ServiceControllerUtils serviceControllerUtils,
                                        CurrentUserService currentUserService,
                                        @Value("${supplier-onboarding.owning-admin-email:}") String owningAdminEmail) {
        this.registrationRepository = registrationRepository;
        this.documentRepository = documentRepository;
        this.superAdminRepository = superAdminRepository;
        this.microvistaService = microvistaService;
        this.workflowEngineService = workflowEngineService;
        this.emailTemplateRenderingService = emailTemplateRenderingService;
        this.emailService = emailService;
        this.serviceControllerUtils = serviceControllerUtils;
        this.currentUserService = currentUserService;
        this.owningAdminEmail = owningAdminEmail;
    }

    /** v1 is single-company/generic - the public registration flow has no logged-in principal
     *  to scope a new draft to, so every draft is owned by one configured admin account
     *  (supplier-onboarding.owning-admin-email), falling back to whichever SuperAdmin row was
     *  created first. Revisit if MDG- ever needs true multi-tenant public registration. */
    private SuperAdmin resolveOwningAdmin() {
        if (owningAdminEmail != null && !owningAdminEmail.isBlank()) {
            Optional<SuperAdmin> byEmail = superAdminRepository.findByEmail(owningAdminEmail);
            if (byEmail.isPresent()) return byEmail.get();
        }
        return superAdminRepository.findAll(Sort.by("superAdminId")).stream().findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No super_admin account exists yet - create one before accepting supplier registrations"));
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String generateResumeCode() {
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // no 0/O/1/I - avoids ambiguity
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(8);
        for (int i = 0; i < 8; i++) sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        return sb.toString();
    }

    private void applyStr(JSONObject json, String key, BiConsumer<SupplierRegistration, String> setter, SupplierRegistration reg) {
        if (json.has(key) && !json.isNull(key)) setter.accept(reg, json.getString(key));
    }

    private void applyBool(JSONObject json, String key, BiConsumer<SupplierRegistration, Boolean> setter, SupplierRegistration reg) {
        if (json.has(key) && !json.isNull(key)) setter.accept(reg, json.getBoolean(key));
    }

    private void applyInt(JSONObject json, String key, BiConsumer<SupplierRegistration, Integer> setter, SupplierRegistration reg) {
        if (json.has(key) && !json.isNull(key)) setter.accept(reg, json.getInt(key));
    }

    private void applyDraftFields(SupplierRegistration reg, JSONObject json) {
        applyStr(json, "vendorName", SupplierRegistration::setVendorName, reg);
        applyStr(json, "address", SupplierRegistration::setAddress, reg);
        applyStr(json, "vendorType", SupplierRegistration::setVendorType, reg);
        applyStr(json, "contactName", SupplierRegistration::setContactName, reg);
        applyStr(json, "designation", SupplierRegistration::setDesignation, reg);
        applyStr(json, "email", SupplierRegistration::setEmail, reg);
        applyStr(json, "phone", SupplierRegistration::setPhone, reg);
        applyStr(json, "contact1Name", SupplierRegistration::setContact1Name, reg);
        applyStr(json, "contact1Role", SupplierRegistration::setContact1Role, reg);
        applyStr(json, "contact1Email", SupplierRegistration::setContact1Email, reg);
        applyStr(json, "contact1Phone", SupplierRegistration::setContact1Phone, reg);
        applyStr(json, "contact2Name", SupplierRegistration::setContact2Name, reg);
        applyStr(json, "contact2Role", SupplierRegistration::setContact2Role, reg);
        applyStr(json, "contact2Email", SupplierRegistration::setContact2Email, reg);
        applyStr(json, "contact2Phone", SupplierRegistration::setContact2Phone, reg);
        applyInt(json, "primaryContact", SupplierRegistration::setPrimaryContact, reg);
        applyStr(json, "businessTypes", SupplierRegistration::setBusinessTypes, reg);
        applyStr(json, "businessScope", SupplierRegistration::setBusinessScope, reg);
        applyStr(json, "companyType", SupplierRegistration::setCompanyType, reg);
        applyStr(json, "telephone", SupplierRegistration::setTelephone, reg);
        applyStr(json, "fax", SupplierRegistration::setFax, reg);
        applyStr(json, "weeklyOff", SupplierRegistration::setWeeklyOff, reg);
        applyStr(json, "annualTurnover", SupplierRegistration::setAnnualTurnover, reg);
        applyStr(json, "turnoverYear", SupplierRegistration::setTurnoverYear, reg);
        applyStr(json, "regulatoryActs", SupplierRegistration::setRegulatoryActs, reg);
        applyStr(json, "manpowerOffice", SupplierRegistration::setManpowerOffice, reg);
        applyStr(json, "manpowerSupervisor", SupplierRegistration::setManpowerSupervisor, reg);
        applyStr(json, "manpowerWorkmen", SupplierRegistration::setManpowerWorkmen, reg);
        applyStr(json, "shiftsPerDay", SupplierRegistration::setShiftsPerDay, reg);
        applyStr(json, "spareCapacity", SupplierRegistration::setSpareCapacity, reg);
        applyStr(json, "floorSpace", SupplierRegistration::setFloorSpace, reg);
        applyStr(json, "equipmentFacilities", SupplierRegistration::setEquipmentFacilities, reg);
        applyStr(json, "directorsJson", SupplierRegistration::setDirectorsJson, reg);
        applyStr(json, "machineryJson", SupplierRegistration::setMachineryJson, reg);
        applyBool(json, "declarationAccepted", SupplierRegistration::setDeclarationAccepted, reg);
        applyBool(json, "dpdpConsentAccepted", SupplierRegistration::setDpdpConsentAccepted, reg);
        applyStr(json, "gstNumber", (r, v) -> r.setGstNumber(v.trim().toUpperCase()), reg);
        applyStr(json, "panNumber", (r, v) -> r.setPanNumber(v.trim().toUpperCase()), reg);
        applyBool(json, "isMsme", SupplierRegistration::setIsMsme, reg);
        applyStr(json, "msmeNumber", SupplierRegistration::setMsmeNumber, reg);
        applyStr(json, "cinNumber", SupplierRegistration::setCinNumber, reg);
        applyStr(json, "foreignTaxId", SupplierRegistration::setForeignTaxId, reg);
        applyStr(json, "foreignRegNo", SupplierRegistration::setForeignRegNo, reg);
        applyStr(json, "beneficiaryName", SupplierRegistration::setBeneficiaryName, reg);
        applyStr(json, "accountNumber", SupplierRegistration::setAccountNumber, reg);
        applyStr(json, "ifscCode", SupplierRegistration::setIfscCode, reg);
        applyStr(json, "bankName", SupplierRegistration::setBankName, reg);
        applyStr(json, "foreignAcctNo", SupplierRegistration::setForeignAcctNo, reg);
        applyStr(json, "swiftCode", SupplierRegistration::setSwiftCode, reg);
        applyStr(json, "isoCertificateNo", SupplierRegistration::setIsoCertificateNo, reg);
        applyStr(json, "isoCertifyingBody", SupplierRegistration::setIsoCertifyingBody, reg);
        applyStr(json, "isoExpiry", SupplierRegistration::setIsoExpiry, reg);
        applyStr(json, "iso14001CertificateNo", SupplierRegistration::setIso14001CertificateNo, reg);
        applyStr(json, "iso14001CertifyingBody", SupplierRegistration::setIso14001CertifyingBody, reg);
        applyStr(json, "iso14001Expiry", SupplierRegistration::setIso14001Expiry, reg);
        applyStr(json, "iso45001CertificateNo", SupplierRegistration::setIso45001CertificateNo, reg);
        applyStr(json, "iso45001CertifyingBody", SupplierRegistration::setIso45001CertifyingBody, reg);
        applyStr(json, "iso45001Expiry", SupplierRegistration::setIso45001Expiry, reg);
        applyStr(json, "iso27001CertificateNo", SupplierRegistration::setIso27001CertificateNo, reg);
        applyStr(json, "iso27001CertifyingBody", SupplierRegistration::setIso27001CertifyingBody, reg);
        applyStr(json, "iso27001Expiry", SupplierRegistration::setIso27001Expiry, reg);
        applyStr(json, "as9100dCertificateNo", SupplierRegistration::setAs9100dCertificateNo, reg);
        applyStr(json, "as9100dCertifyingBody", SupplierRegistration::setAs9100dCertifyingBody, reg);
        applyStr(json, "as9100dExpiry", SupplierRegistration::setAs9100dExpiry, reg);
        applyStr(json, "nadcapCertificateNo", SupplierRegistration::setNadcapCertificateNo, reg);
        applyStr(json, "nadcapExpiry", SupplierRegistration::setNadcapExpiry, reg);
    }

    @Transactional
    public ServiceResponse saveDraft(Map<String, Object> body) {
        ServiceResponse response = new ServiceResponse();
        try {
            JSONObject json = new JSONObject(body);
            Long registrationId = json.has("registrationId") && !json.isNull("registrationId")
                    ? json.getLong("registrationId") : null;

            SupplierRegistration reg;
            boolean isNew = registrationId == null;
            if (isNew) {
                reg = new SupplierRegistration();
                reg.setSuperAdmin(resolveOwningAdmin());
                reg.setResumeCode(generateResumeCode());
            } else {
                reg = registrationRepository.findById(registrationId)
                        .orElseThrow(() -> new RuntimeException("Registration not found"));
                if (!EDITABLE_STATUSES.contains(reg.getStatus())) {
                    return serviceControllerUtils.prepareMobileResponseErrorStatus(response, AppConstants.ERRORCODE,
                            "This application has already been submitted for review and can no longer be edited.");
                }
            }

            applyDraftFields(reg, json);
            reg = registrationRepository.save(reg);

            if (isNew && !isBlank(reg.getContact1Email() != null ? reg.getContact1Email() : reg.getEmail())) {
                sendDraftSavedEmail(reg);
            }

            response.addData("registrationId", reg.getId());
            response.addData("resumeCode", reg.getResumeCode());
            return serviceControllerUtils.prepareMobileResponseSuccessStatus(response, AppConstants.SUCCESSCODE, "Draft saved");
        } catch (Exception e) {
            logger.error("saveDraft failed", e);
            return serviceControllerUtils.prepareMobileResponseErrorStatus(response, AppConstants.ERRORCODE, "Failed to save draft: " + e.getMessage());
        }
    }

    private void sendDraftSavedEmail(SupplierRegistration reg) {
        try {
            String toEmail = !isBlank(reg.getContact1Email()) ? reg.getContact1Email() : reg.getEmail();
            if (isBlank(toEmail)) return;
            Map<String, Object> vars = new HashMap<>();
            vars.put("contactName", reg.getContactName() != null ? reg.getContactName() : "there");
            vars.put("resumeCode", reg.getResumeCode());
            var rendered = emailTemplateRenderingService.render("SUPPLIER.DRAFT_SAVED", vars);
            emailService.sendHtmlEmail(toEmail, rendered.subject(), rendered.html(), rendered.plainText());
        } catch (Exception e) {
            logger.warn("Could not send draft-saved email for registration {}", reg.getId(), e);
        }
    }

    public ServiceResponse getDraftByCode(String resumeCode) {
        ServiceResponse response = new ServiceResponse();
        try {
            SupplierRegistration reg = registrationRepository.findByResumeCode(resumeCode)
                    .orElseThrow(() -> new RuntimeException("No draft found for this resume code"));
            response.addData("registration", reg);
            return serviceControllerUtils.prepareMobileResponseSuccessStatus(response, AppConstants.SUCCESSCODE, "Found");
        } catch (Exception e) {
            return serviceControllerUtils.prepareMobileResponseErrorStatus(response, AppConstants.ERRORCODE, e.getMessage());
        }
    }

    /** Submitted and escalated registrations awaiting a decision, scoped to the current
     *  approver/super_admin's tenant - the reviewer queue. */
    public ServiceResponse listForReview() {
        ServiceResponse response = new ServiceResponse();
        Long superAdminId = currentUserService.getOwningSuperAdminId();
        var submitted = registrationRepository.findBySuperAdmin_SuperAdminIdAndStatus(
                superAdminId, SupplierRegistrationStatus.REGISTRATION_SUBMITTED);
        var escalated = registrationRepository.findBySuperAdmin_SuperAdminIdAndStatus(
                superAdminId, SupplierRegistrationStatus.ESCALATED);
        response.addData("submitted", submitted);
        response.addData("escalated", escalated);
        return serviceControllerUtils.prepareMobileResponseSuccessStatus(response, AppConstants.SUCCESSCODE, "Found");
    }

    public ServiceResponse getRegistration(Long registrationId) {
        ServiceResponse response = new ServiceResponse();
        try {
            SupplierRegistration reg = registrationRepository.findById(registrationId)
                    .orElseThrow(() -> new RuntimeException("Registration not found"));
            response.addData("registration", reg);
            return serviceControllerUtils.prepareMobileResponseSuccessStatus(response, AppConstants.SUCCESSCODE, "Found");
        } catch (Exception e) {
            return serviceControllerUtils.prepareMobileResponseErrorStatus(response, AppConstants.ERRORCODE, e.getMessage());
        }
    }

    @Transactional
    public ServiceResponse submit(Long registrationId) {
        ServiceResponse response = new ServiceResponse();
        try {
            SupplierRegistration reg = registrationRepository.findById(registrationId)
                    .orElseThrow(() -> new RuntimeException("Registration not found"));

            if (!EDITABLE_STATUSES.contains(reg.getStatus())) {
                return serviceControllerUtils.prepareMobileResponseErrorStatus(response, AppConstants.ERRORCODE,
                        "This application has already been submitted for approval.");
            }

            java.util.List<String> missing = new java.util.ArrayList<>();
            for (SupplierDocumentConfig.DocDef d : SupplierDocumentConfig.docsFor(reg.getVendorType())) {
                boolean required = d.required() || (Boolean.TRUE.equals(reg.getIsMsme()) && "udyam".equals(d.id()));
                if (!required) continue;
                boolean present = hasDocument(reg.getId(), d.id());
                if (!present) missing.add(d.name());
            }
            if (isBlank(reg.getContact1Email()) && isBlank(reg.getEmail())) missing.add(0, "a contact email");
            if (reg.getEmail() != null && reg.getEmail().contains("@placeholder.local")) missing.add(0, "a real contact email");
            if (!"FOREIGN".equals(reg.getVendorType()) && isBlank(reg.getCompanyType())) missing.add("entity type");
            if (!Boolean.TRUE.equals(reg.getDpdpConsentAccepted())) missing.add("data protection consent");
            if (!missing.isEmpty()) {
                return serviceControllerUtils.prepareMobileResponseErrorStatus(response, AppConstants.ERRORCODE,
                        "Missing required: " + String.join(", ", missing));
            }

            java.util.List<String> mismatches = new java.util.ArrayList<>();
            String pan = Optional.ofNullable(reg.getPanNumber()).orElse("").trim().toUpperCase();
            String gstin = Optional.ofNullable(reg.getGstNumber()).orElse("").trim().toUpperCase();
            if (pan.length() == 10 && gstin.length() == 15 && !gstin.substring(2, 12).equals(pan)) {
                mismatches.add("the PAN inside your GSTIN does not match your PAN card");
            }
            String expectedLetter = switch (Optional.ofNullable(reg.getCompanyType()).orElse("")) {
                case "Proprietary" -> "P";
                case "Partnership" -> "F";
                case "Private Limited", "Public Limited" -> "C";
                default -> null;
            };
            if (expectedLetter != null && pan.length() == 10 && !pan.substring(3, 4).equals(expectedLetter)) {
                mismatches.add("your PAN does not match the entity type you selected");
            }
            if (!mismatches.isEmpty()) {
                return serviceControllerUtils.prepareMobileResponseErrorStatus(response, AppConstants.ERRORCODE,
                        "Not matched: " + String.join("; ", mismatches));
            }

            if (!isBlank(reg.getPanNumber()) && !isBlank(reg.getGstNumber())) {
                try {
                    VerifyResult match = microvistaService.verifyPanGstMatch(reg.getPanNumber(), reg.getGstNumber());
                    if (!match.isVerified()) {
                        return serviceControllerUtils.prepareMobileResponseErrorStatus(response, AppConstants.ERRORCODE,
                                "The GST number does not match the PAN on this application. Please check both and try again.");
                    }
                } catch (Exception e) {
                    logger.warn("PAN-to-GST check could not be completed for registration {}; allowing submission without it", registrationId, e);
                }
            }

            if (isBlank(reg.getVendorName()) && !isBlank(reg.getBeneficiaryName())) {
                reg.setVendorName(reg.getBeneficiaryName());
            }

            JSONObject metadata = new JSONObject();
            metadata.put("registrationId", reg.getId());
            metadata.put("vendorName", reg.getVendorName() != null ? reg.getVendorName() : "your company");
            metadata.put("contactName", reg.getContactName() != null ? reg.getContactName() : "there");
            metadata.put("email", reg.getEmail());
            metadata.put("phone", reg.getPhone());
            metadata.put("vendorType", reg.getVendorType());
            metadata.put("gstNumber", reg.getGstNumber());
            metadata.put("panNumber", reg.getPanNumber());

            var workflowRequest = workflowEngineService.createRequest(
                    "VENDOR_ONBOARDING", "Supplier registration - " + reg.getVendorName(), metadata, null);

            reg.setStatus(SupplierRegistrationStatus.REGISTRATION_SUBMITTED);
            reg.setWorkflowRequest(workflowRequest);
            registrationRepository.save(reg);

            sendSubmittedEmail(reg);

            response.addData("registrationId", reg.getId());
            response.addData("workflowRequestId", workflowRequest.getId());
            return serviceControllerUtils.prepareMobileResponseSuccessStatus(response, AppConstants.SUCCESSCODE, "Submitted for review");
        } catch (Exception e) {
            logger.error("Submit failed for registrationId={}", registrationId, e);
            return serviceControllerUtils.prepareMobileResponseErrorStatus(response, AppConstants.ERRORCODE, "Failed to submit: " + e.getMessage());
        }
    }

    private void sendSubmittedEmail(SupplierRegistration reg) {
        try {
            Map<String, Object> vars = new HashMap<>();
            vars.put("contactName", reg.getContactName() != null ? reg.getContactName() : "there");
            vars.put("vendorName", reg.getVendorName());
            var rendered = emailTemplateRenderingService.render("SUPPLIER.SUBMITTED", vars);
            emailService.sendHtmlEmail(reg.getEmail(), rendered.subject(), rendered.html(), rendered.plainText());
        } catch (Exception e) {
            logger.warn("Could not send submitted email for registration {}", reg.getId(), e);
        }
    }

    private boolean hasDocument(Long registrationId, String docType) {
        return documentRepository.findByRegistration_IdAndDocType(registrationId, docType).isPresent();
    }
}
