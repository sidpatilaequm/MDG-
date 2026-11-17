package com.example.multimedia.file_upload_api.service.supplier;

import com.example.multimedia.file_upload_api.dto.ServiceResponse;
import com.example.multimedia.file_upload_api.entity.CompanyDetails;
import com.example.multimedia.file_upload_api.entity.ChequeDetails;
import com.example.multimedia.file_upload_api.entity.supplier.SupplierChangeRequest;
import com.example.multimedia.file_upload_api.entity.supplier.SupplierCompanyProfile;
import com.example.multimedia.file_upload_api.entity.supplier.SupplierRegistration;
import com.example.multimedia.file_upload_api.entity.supplier.SupplierRegistrationStatus;
import com.example.multimedia.file_upload_api.repository.ChequeDetailsRepository;
import com.example.multimedia.file_upload_api.repository.CompanyDetailsRepository;
import com.example.multimedia.file_upload_api.repository.supplier.SupplierChangeRequestRepository;
import com.example.multimedia.file_upload_api.repository.supplier.SupplierCompanyProfileRepository;
import com.example.multimedia.file_upload_api.repository.supplier.SupplierRegistrationRepository;
import com.example.multimedia.file_upload_api.service.CurrentUserService;
import com.example.multimedia.file_upload_api.service.EmailService;
import com.example.multimedia.file_upload_api.service.email.EmailTemplateRenderingService;
import com.example.multimedia.file_upload_api.service.workflow.WorkflowEngineService;
import com.example.multimedia.file_upload_api.utils.AppConstants;
import com.example.multimedia.file_upload_api.utils.ServiceControllerUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Post-approval profile edits for an already-ACTIVE vendor - routed through the same
 * {@code VENDOR_CHANGE_REQUEST} workflow (seeded by V8) rather than applied directly, so a
 * change still goes through approval.
 */
@Service
public class SupplierChangeRequestService {

    private static final Logger logger = LoggerFactory.getLogger(SupplierChangeRequestService.class);

    /** Field name -> which entity holds it. Deliberately a small allow-list, not every
     *  SupplierCompanyProfile column - only fields a vendor should reasonably self-service. */
    private static final Set<String> ALLOWED_FIELDS = Set.of(
            "contactName", "designation", "phone", "contact1Email", "contact1Phone", "businessScope",
            "bankName", "accountNumber", "ifscCode", "address");

    private final SupplierRegistrationRepository registrationRepository;
    private final SupplierChangeRequestRepository changeRequestRepository;
    private final SupplierCompanyProfileRepository companyProfileRepository;
    private final CompanyDetailsRepository companyDetailsRepository;
    private final ChequeDetailsRepository chequeDetailsRepository;
    private final WorkflowEngineService workflowEngineService;
    private final EmailTemplateRenderingService emailTemplateRenderingService;
    private final EmailService emailService;
    private final ServiceControllerUtils serviceControllerUtils;
    private final CurrentUserService currentUserService;

    public SupplierChangeRequestService(SupplierRegistrationRepository registrationRepository,
                                         SupplierChangeRequestRepository changeRequestRepository,
                                         SupplierCompanyProfileRepository companyProfileRepository,
                                         CompanyDetailsRepository companyDetailsRepository,
                                         ChequeDetailsRepository chequeDetailsRepository,
                                         WorkflowEngineService workflowEngineService,
                                         EmailTemplateRenderingService emailTemplateRenderingService,
                                         EmailService emailService,
                                         ServiceControllerUtils serviceControllerUtils,
                                         CurrentUserService currentUserService) {
        this.registrationRepository = registrationRepository;
        this.changeRequestRepository = changeRequestRepository;
        this.companyProfileRepository = companyProfileRepository;
        this.companyDetailsRepository = companyDetailsRepository;
        this.chequeDetailsRepository = chequeDetailsRepository;
        this.workflowEngineService = workflowEngineService;
        this.emailTemplateRenderingService = emailTemplateRenderingService;
        this.emailService = emailService;
        this.serviceControllerUtils = serviceControllerUtils;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public ServiceResponse submit(Long registrationId, String fieldName, String newValue) {
        ServiceResponse response = new ServiceResponse();
        try {
            SupplierRegistration reg = registrationRepository.findById(registrationId)
                    .orElseThrow(() -> new RuntimeException("Registration not found"));
            Long currentUserId = currentUserService.getCurrentPrincipalIdentity().id();
            if (!currentUserId.equals(reg.getUserId())) {
                return serviceControllerUtils.prepareMobileResponseErrorStatus(response, AppConstants.ERRORCODE,
                        "You can only request changes on your own vendor profile.");
            }
            if (reg.getStatus() != SupplierRegistrationStatus.ACTIVE) {
                return serviceControllerUtils.prepareMobileResponseErrorStatus(response, AppConstants.ERRORCODE,
                        "Only an active, approved vendor can request a profile change.");
            }
            if (!ALLOWED_FIELDS.contains(fieldName)) {
                return serviceControllerUtils.prepareMobileResponseErrorStatus(response, AppConstants.ERRORCODE,
                        "This field cannot be changed through a change request.");
            }

            String oldValue = currentValueOf(reg, fieldName);

            SupplierChangeRequest cr = new SupplierChangeRequest();
            cr.setRegistration(reg);
            cr.setFieldName(fieldName);
            cr.setOldValue(oldValue);
            cr.setNewValue(newValue);
            cr.setStatus("PENDING");
            cr = changeRequestRepository.save(cr);

            JSONObject metadata = new JSONObject();
            metadata.put("registrationId", reg.getId());
            metadata.put("vendorName", reg.getVendorName());
            metadata.put("fieldName", fieldName);
            metadata.put("oldValue", oldValue);
            metadata.put("newValue", newValue);

            var workflowRequest = workflowEngineService.createRequest("VENDOR_CHANGE_REQUEST",
                    "Change request - " + fieldName + " for " + reg.getVendorName(), metadata, reg.getUserId());
            cr.setWorkflowRequest(workflowRequest);
            changeRequestRepository.save(cr);

            sendSubmittedEmail(reg, fieldName, oldValue, newValue);

            response.addData("changeRequestId", cr.getId());
            response.addData("workflowRequestId", workflowRequest.getId());
            return serviceControllerUtils.prepareMobileResponseSuccessStatus(response, AppConstants.SUCCESSCODE, "Change request submitted");
        } catch (Exception e) {
            logger.error("Change request submission failed for registrationId={}", registrationId, e);
            return serviceControllerUtils.prepareMobileResponseErrorStatus(response, AppConstants.ERRORCODE, "Failed: " + e.getMessage());
        }
    }

    private String currentValueOf(SupplierRegistration reg, String fieldName) {
        Optional<SupplierCompanyProfile> profileOpt = companyProfileRepository.findByRegistration_Id(reg.getId());
        Optional<ChequeDetails> chequeOpt = reg.getCompanyId() != null
                ? chequeDetailsRepository.findByCompany_CompanyId(reg.getCompanyId()) : Optional.empty();
        Optional<CompanyDetails> companyOpt = reg.getCompanyId() != null
                ? companyDetailsRepository.findById(reg.getCompanyId()) : Optional.empty();

        return switch (fieldName) {
            case "contactName" -> profileOpt.map(SupplierCompanyProfile::getContactName).orElse(null);
            case "designation" -> profileOpt.map(SupplierCompanyProfile::getDesignation).orElse(null);
            case "phone" -> profileOpt.map(SupplierCompanyProfile::getPhone).orElse(null);
            case "contact1Email" -> profileOpt.map(SupplierCompanyProfile::getContact1Email).orElse(null);
            case "contact1Phone" -> profileOpt.map(SupplierCompanyProfile::getContact1Phone).orElse(null);
            case "businessScope" -> profileOpt.map(SupplierCompanyProfile::getBusinessScope).orElse(null);
            case "bankName" -> chequeOpt.map(ChequeDetails::getBank).orElse(null);
            case "accountNumber" -> chequeOpt.map(ChequeDetails::getAccountNumber).orElse(null);
            case "ifscCode" -> chequeOpt.map(ChequeDetails::getIfsc).orElse(null);
            case "address" -> companyOpt.map(CompanyDetails::getRegisteredAddress).orElse(null);
            default -> null;
        };
    }

    /** Called by SupplierOnboardingProvisioningService once this change request's workflow
     *  request resolves as approved - applies newValue to whichever entity actually owns the
     *  field. */
    @Transactional
    public void applyApprovedChange(SupplierChangeRequest cr) {
        SupplierRegistration reg = cr.getRegistration();
        String field = cr.getFieldName();
        String newValue = cr.getNewValue();

        switch (field) {
            case "contactName" -> updateProfile(reg, p -> p.setContactName(newValue));
            case "designation" -> updateProfile(reg, p -> p.setDesignation(newValue));
            case "phone" -> updateProfile(reg, p -> p.setPhone(newValue));
            case "contact1Email" -> updateProfile(reg, p -> p.setContact1Email(newValue));
            case "contact1Phone" -> updateProfile(reg, p -> p.setContact1Phone(newValue));
            case "businessScope" -> updateProfile(reg, p -> p.setBusinessScope(newValue));
            case "bankName" -> updateCheque(reg, c -> c.setBank(newValue));
            case "accountNumber" -> updateCheque(reg, c -> c.setAccountNumber(newValue));
            case "ifscCode" -> updateCheque(reg, c -> c.setIfsc(newValue));
            case "address" -> updateCompany(reg, c -> c.setRegisteredAddress(newValue));
            default -> logger.warn("No apply handler for change-request field '{}' on registration {}", field, reg.getId());
        }

        cr.setStatus("APPROVED");
        changeRequestRepository.save(cr);
        sendDecidedEmail(reg, field, "approved");
    }

    @Transactional
    public void rejectChange(SupplierChangeRequest cr) {
        cr.setStatus("REJECTED");
        changeRequestRepository.save(cr);
        sendDecidedEmail(cr.getRegistration(), cr.getFieldName(), "rejected");
    }

    private void updateProfile(SupplierRegistration reg, java.util.function.Consumer<SupplierCompanyProfile> mutator) {
        companyProfileRepository.findByRegistration_Id(reg.getId()).ifPresent(profile -> {
            mutator.accept(profile);
            companyProfileRepository.save(profile);
        });
    }

    private void updateCheque(SupplierRegistration reg, java.util.function.Consumer<ChequeDetails> mutator) {
        if (reg.getCompanyId() == null) return;
        chequeDetailsRepository.findByCompany_CompanyId(reg.getCompanyId()).ifPresent(cheque -> {
            mutator.accept(cheque);
            chequeDetailsRepository.save(cheque);
        });
    }

    private void updateCompany(SupplierRegistration reg, java.util.function.Consumer<CompanyDetails> mutator) {
        if (reg.getCompanyId() == null) return;
        companyDetailsRepository.findById(reg.getCompanyId()).ifPresent(company -> {
            mutator.accept(company);
            companyDetailsRepository.save(company);
        });
    }

    private void sendSubmittedEmail(SupplierRegistration reg, String fieldName, String oldValue, String newValue) {
        try {
            Map<String, Object> vars = new HashMap<>();
            vars.put("contactName", reg.getContactName() != null ? reg.getContactName() : "there");
            vars.put("fieldName", fieldName);
            vars.put("oldValue", oldValue);
            vars.put("newValue", newValue);
            var rendered = emailTemplateRenderingService.render("SUPPLIER.CHANGE_REQUEST_SUBMITTED", vars);
            emailService.sendHtmlEmail(reg.getEmail(), rendered.subject(), rendered.html(), rendered.plainText());
        } catch (Exception e) {
            logger.warn("Could not send change-request-submitted email for registration {}", reg.getId(), e);
        }
    }

    private void sendDecidedEmail(SupplierRegistration reg, String fieldName, String decision) {
        try {
            Map<String, Object> vars = new HashMap<>();
            vars.put("contactName", reg.getContactName() != null ? reg.getContactName() : "there");
            vars.put("fieldName", fieldName);
            vars.put("decision", decision);
            var rendered = emailTemplateRenderingService.render("SUPPLIER.CHANGE_REQUEST_DECIDED", vars);
            emailService.sendHtmlEmail(reg.getEmail(), rendered.subject(), rendered.html(), rendered.plainText());
        } catch (Exception e) {
            logger.warn("Could not send change-request-decided email for registration {}", reg.getId(), e);
        }
    }
}
