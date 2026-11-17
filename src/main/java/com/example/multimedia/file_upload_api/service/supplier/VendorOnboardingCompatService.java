package com.example.multimedia.file_upload_api.service.supplier;

import com.example.multimedia.file_upload_api.dto.ServiceResponse;
import com.example.multimedia.file_upload_api.entity.supplier.SupplierRegistration;
import com.example.multimedia.file_upload_api.entity.workflow.ApprovalDecision;
import com.example.multimedia.file_upload_api.entity.workflow.RequestStage;
import com.example.multimedia.file_upload_api.entity.workflow.WorkflowRequest;
import com.example.multimedia.file_upload_api.repository.supplier.SupplierRegistrationRepository;
import com.example.multimedia.file_upload_api.repository.workflow.RequestStageRepository;
import com.example.multimedia.file_upload_api.service.CurrentUserService;
import com.example.multimedia.file_upload_api.service.workflow.StageAdvancementService;
import com.example.multimedia.file_upload_api.utils.AppConstants;
import com.example.multimedia.file_upload_api.utils.ServiceControllerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Thin translation layer between Django's vendor-onboarding field/data model and the
 * existing supplier-registration + approval-workflow engine (built separately, same
 * underlying tables). Every field mapping and status string here was confirmed by reading
 * Django's pages/views.py handling of each response field directly - see the approved plan
 * (we-dont-need-to-linked-rossum.md), Phase 4.
 */
@Service
public class VendorOnboardingCompatService {

    private static final Logger logger = LoggerFactory.getLogger(VendorOnboardingCompatService.class);

    private final SupplierRegistrationService registrationService;
    private final SupplierRegistrationRepository registrationRepository;
    private final RequestStageRepository requestStageRepository;
    private final StageAdvancementService stageAdvancementService;
    private final CurrentUserService currentUserService;
    private final ServiceControllerUtils serviceControllerUtils;

    public VendorOnboardingCompatService(SupplierRegistrationService registrationService,
                                          SupplierRegistrationRepository registrationRepository,
                                          RequestStageRepository requestStageRepository,
                                          StageAdvancementService stageAdvancementService,
                                          CurrentUserService currentUserService,
                                          ServiceControllerUtils serviceControllerUtils) {
        this.registrationService = registrationService;
        this.registrationRepository = registrationRepository;
        this.requestStageRepository = requestStageRepository;
        this.stageAdvancementService = stageAdvancementService;
        this.currentUserService = currentUserService;
        this.serviceControllerUtils = serviceControllerUtils;
    }

    /** POST /api/public/registration/onboarding-request. Stops at DRAFT, not submit() - Django
     *  sends none of submit()'s required fields (documents, DPDP consent, entity type) yet.
     *  adminId is received and ignored (v1 is single-tenant; SupplierRegistrationService's own
     *  resolveOwningAdmin() already handles this). password is deliberately omitted from the
     *  response - Django already falls back to "User@123" when it's missing. */
    public ServiceResponse onboardingRequest(Map<String, Object> djangoBody) {
        ServiceResponse response = new ServiceResponse();
        try {
            Map<String, Object> draftBody = new HashMap<>();
            putIfPresent(djangoBody, draftBody, "vendorLegalEntityName", "vendorName");
            putIfPresent(djangoBody, draftBody, "vendorAddress", "address");
            putIfPresent(djangoBody, draftBody, "vendorContactName", "contactName");
            putIfPresent(djangoBody, draftBody, "vendorDesignation", "designation");
            putIfPresent(djangoBody, draftBody, "vendorEmail", "email");
            putIfPresent(djangoBody, draftBody, "vendorPhoneNumber", "phone");

            ServiceResponse draftResponse = registrationService.saveDraft(draftBody);
            if (!AppConstants.SUCCESSCODE.equals(draftResponse.getStatus())) {
                return serviceControllerUtils.prepareMobileResponseErrorStatus(response, AppConstants.ERRORCODE,
                        draftResponse.getStatusMsg());
            }

            Object registrationId = draftResponse.getData().get("registrationId");
            Map<String, Object> onboardingRequest = new HashMap<>();
            onboardingRequest.put("userId", registrationId);
            onboardingRequest.put("onboardingStatus", "DRAFT");
            response.addData("onboardingRequest", onboardingRequest);
            return serviceControllerUtils.prepareMobileResponseSuccessStatus(response, AppConstants.SUCCESSCODE, "Onboarding request created");
        } catch (Exception e) {
            logger.error("onboardingRequest failed", e);
            return serviceControllerUtils.prepareMobileResponseErrorStatus(response, AppConstants.ERRORCODE, e.getMessage());
        }
    }

    private void putIfPresent(Map<String, Object> source, Map<String, Object> target, String sourceKey, String targetKey) {
        if (source.containsKey(sourceKey) && source.get(sourceKey) != null) {
            target.put(targetKey, source.get(sourceKey));
        }
    }

    /** GET /api/vendor-onboarding/requests - thin translation of the existing, already
     *  tenant-scoped SupplierRegistrationService.listForReview(). */
    @SuppressWarnings("unchecked")
    public ServiceResponse listRequests() {
        ServiceResponse response = new ServiceResponse();
        ServiceResponse reviewResponse = registrationService.listForReview();

        List<SupplierRegistration> submitted = (List<SupplierRegistration>) reviewResponse.getData().get("submitted");
        List<SupplierRegistration> escalated = (List<SupplierRegistration>) reviewResponse.getData().get("escalated");

        List<Map<String, Object>> requests = new ArrayList<>();
        if (submitted != null) submitted.forEach(reg -> requests.add(toOnboardingRequestShape(reg)));
        if (escalated != null) escalated.forEach(reg -> requests.add(toOnboardingRequestShape(reg)));

        response.addData("requests", requests);
        return serviceControllerUtils.prepareMobileResponseSuccessStatus(response, AppConstants.SUCCESSCODE, "Found");
    }

    private Map<String, Object> toOnboardingRequestShape(SupplierRegistration reg) {
        Map<String, Object> shape = new HashMap<>();
        shape.put("vendorEmail", reg.getEmail());
        shape.put("vendorLegalEntityName", reg.getVendorName());
        shape.put("vendorAddress", reg.getAddress());
        shape.put("vendorContactName", reg.getContactName());
        shape.put("vendorDesignation", reg.getDesignation());
        shape.put("vendorPhoneNumber", reg.getPhone());
        shape.put("onboardingStatus", reg.getStatus().name());
        shape.put("userId", reg.getId());
        return shape;
    }

    /** GET /api/public/registration/onboarding-details?token= - Django stores whatever string
     *  this service returns as onboardingToken verbatim and echoes it back later as token, so
     *  the existing resumeCode (already unique, already generated by saveDraft) serves as the
     *  token with zero new lookup logic. */
    public ServiceResponse onboardingDetails(String token) {
        ServiceResponse response = new ServiceResponse();
        try {
            SupplierRegistration reg = registrationRepository.findByResumeCode(token)
                    .orElseThrow(() -> new RuntimeException("No onboarding request found for this token"));

            Map<String, Object> details = new HashMap<>();
            details.put("vendorEmail", reg.getEmail());
            details.put("vendorLegalEntityName", reg.getVendorName());
            details.put("vendorAddress", reg.getAddress());
            details.put("vendorContactName", reg.getContactName());
            details.put("vendorDesignation", reg.getDesignation());
            details.put("vendorPhoneNumber", reg.getPhone());
            details.put("userId", reg.getId());

            response.addData("onboardingDetails", details);
            return serviceControllerUtils.prepareMobileResponseSuccessStatus(response, AppConstants.SUCCESSCODE, "Found");
        } catch (Exception e) {
            return serviceControllerUtils.prepareMobileResponseErrorStatus(response, AppConstants.ERRORCODE, e.getMessage());
        }
    }

    /** POST /api/vendor-onboarding/generate-link/{user_id} - this IS the approval action
     *  (Django's reject path never calls Java at all, only approve does). user_id path
     *  variable is SupplierRegistration.id.
     *
     *  Deliberately NOT @Transactional: recordAction is its own @Transactional unit and must
     *  actually commit before SupplierOnboardingProvisioningService's AFTER_COMMIT listener
     *  runs - wrapping this method would defer that commit past the point where we re-read
     *  post-approval state below. */
    public ServiceResponse generateLink(Long userId) {
        ServiceResponse response = new ServiceResponse();
        try {
            SupplierRegistration reg = registrationRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Onboarding request not found"));

            WorkflowRequest workflowRequest = reg.getWorkflowRequest();
            if (workflowRequest == null || workflowRequest.getCurrentStageOrder() == null) {
                return serviceControllerUtils.prepareMobileResponseErrorStatus(response, AppConstants.ERRORCODE,
                        "This onboarding request has not been submitted into the approval workflow yet");
            }

            RequestStage stage = requestStageRepository.findByWorkflowRequest_IdAndStageOrder(
                            workflowRequest.getId(), workflowRequest.getCurrentStageOrder())
                    .orElseThrow(() -> new RuntimeException("No pending approval stage found for this request"));

            var principal = currentUserService.getCurrentPrincipalIdentity();
            stageAdvancementService.recordAction(stage.getId(), principal.id(), principal.email(),
                    ApprovalDecision.APPROVE, "Approved via vendor-onboarding generate-link", false);

            SupplierRegistration updated = registrationRepository.findById(userId).orElseThrow();
            Map<String, Object> linkData = new HashMap<>();
            linkData.put("onboardingToken", updated.getResumeCode());
            linkData.put("onboardingStatus", updated.getStatus().name());

            response.addData("linkData", linkData);
            return serviceControllerUtils.prepareMobileResponseSuccessStatus(response, AppConstants.SUCCESSCODE, "Approved");
        } catch (IllegalStateException e) {
            // Most commonly: the calling admin isn't a frozen-snapshot member of this
            // request's approver group - i.e. they were added to "Default Approvers" after
            // this particular registration was submitted. Name the actual cause, not a 500.
            return serviceControllerUtils.prepareMobileResponseErrorStatus(response, AppConstants.ERRORCODE, e.getMessage());
        } catch (Exception e) {
            logger.error("generateLink failed for registrationId={}", userId, e);
            return serviceControllerUtils.prepareMobileResponseErrorStatus(response, AppConstants.ERRORCODE, e.getMessage());
        }
    }
}
