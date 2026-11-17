package com.example.multimedia.file_upload_api.service.supplier;

import com.example.multimedia.file_upload_api.entity.Authorization;
import com.example.multimedia.file_upload_api.entity.SuperAdmin;
import com.example.multimedia.file_upload_api.entity.email.EmailFooter;
import com.example.multimedia.file_upload_api.entity.email.EmailTemplate;
import com.example.multimedia.file_upload_api.entity.workflow.*;
import com.example.multimedia.file_upload_api.repository.AuthorizationRepository;
import com.example.multimedia.file_upload_api.repository.SuperAdminRepository;
import com.example.multimedia.file_upload_api.repository.email.EmailFooterRepository;
import com.example.multimedia.file_upload_api.repository.email.EmailTemplateRepository;
import com.example.multimedia.file_upload_api.repository.workflow.ApproverGroupRepository;
import com.example.multimedia.file_upload_api.repository.workflow.WorkflowRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Seeds the fixed rows the supplier-onboarding feature needs to function at all: the
 * "approver" role, the email templates, and the v1 single-company/generic workflows
 * (one linear "Default Approvers" chain for onboarding and for change requests).
 *
 * Runs once the full application context is up ({@link ApplicationReadyEvent}) rather than
 * as a Flyway migration - Flyway always runs before Hibernate's ddl-auto, so a migration
 * seeding a brand-new table would fail on the very deploy that introduces it, before
 * ddl-auto has had a chance to create that table. Every check here is "insert only if
 * missing", so this is safe to run on every single boot.
 */
@Component
public class SupplierOnboardingDataSeeder {

    private static final Logger logger = LoggerFactory.getLogger(SupplierOnboardingDataSeeder.class);

    private final AuthorizationRepository authorizationRepository;
    private final EmailTemplateRepository emailTemplateRepository;
    private final EmailFooterRepository emailFooterRepository;
    private final WorkflowRepository workflowRepository;
    private final ApproverGroupRepository approverGroupRepository;
    private final SuperAdminRepository superAdminRepository;

    public SupplierOnboardingDataSeeder(AuthorizationRepository authorizationRepository,
                                         EmailTemplateRepository emailTemplateRepository,
                                         EmailFooterRepository emailFooterRepository,
                                         WorkflowRepository workflowRepository,
                                         ApproverGroupRepository approverGroupRepository,
                                         SuperAdminRepository superAdminRepository) {
        this.authorizationRepository = authorizationRepository;
        this.emailTemplateRepository = emailTemplateRepository;
        this.emailFooterRepository = emailFooterRepository;
        this.workflowRepository = workflowRepository;
        this.approverGroupRepository = approverGroupRepository;
        this.superAdminRepository = superAdminRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void seed() {
        seedApproverAuthorization();
        seedEmailFooter();
        seedEmailTemplates();
        seedGenericWorkflows();
    }

    private void seedApproverAuthorization() {
        if (authorizationRepository.findByAuthKeyIgnoreCase("approver").isPresent()) return;
        Authorization auth = new Authorization();
        auth.setAuthName("Approver");
        auth.setAuthKey("approver");
        auth.setActive(true);
        authorizationRepository.save(auth);
        logger.info("Seeded 'approver' authorization role");
    }

    private void seedEmailFooter() {
        if (emailFooterRepository.existsById(1L)) return;
        EmailFooter footer = new EmailFooter();
        footer.setId(1L);
        footer.setHtmlFooter("<p style=\"color:#888;font-size:12px;\">This is an automated message - please do not reply.</p>");
        footer.setPlainTextFooter("This is an automated message - please do not reply.");
        emailFooterRepository.save(footer);
    }

    private void seedEmailTemplates() {
        seedTemplate("SUPPLIER.DRAFT_SAVED", "Resume your supplier registration",
                "<p>Hi {{contactName}},</p><p>Your supplier registration draft has been saved. Use the code below to resume it any time:</p><p style=\"font-size:20px;font-weight:bold;\">{{resumeCode}}</p>",
                "Hi {{contactName}}, your supplier registration draft has been saved. Resume code: {{resumeCode}}");

        seedTemplate("SUPPLIER.SUBMITTED", "Your supplier registration has been submitted",
                "<p>Hi {{contactName}},</p><p>Thank you - your application for <strong>{{vendorName}}</strong> has been submitted and is now under review. We will email you once a decision has been made.</p>",
                "Hi {{contactName}}, your application for {{vendorName}} has been submitted and is now under review.");

        seedTemplate("SUPPLIER.APPROVED", "Your supplier account is approved",
                "<p>Hi {{contactName}},</p><p>Congratulations - <strong>{{vendorName}}</strong> has been approved as a supplier. Your vendor code is <strong>{{vendorCode}}</strong>.</p><p>Login email: {{loginEmail}}<br/>Temporary password: {{tempPassword}}</p><p>Please log in and change your password as soon as possible.</p>",
                "Hi {{contactName}}, {{vendorName}} has been approved. Vendor code: {{vendorCode}}. Login email: {{loginEmail}}, temporary password: {{tempPassword}}.");

        seedTemplate("SUPPLIER.REJECTED", "Your supplier registration needs attention",
                "<p>Hi {{contactName}},</p><p>Your application for <strong>{{vendorName}}</strong> was not approved as submitted.</p><p>Reviewer notes: {{rejectionReason}}</p><p>You can log back in with your resume code and correct the application for re-submission.</p>",
                "Hi {{contactName}}, your application for {{vendorName}} was not approved. Reviewer notes: {{rejectionReason}}. You can correct and resubmit.");

        seedTemplate("SUPPLIER.CHANGE_REQUEST_SUBMITTED", "Your profile change request has been submitted",
                "<p>Hi {{contactName}},</p><p>Your request to change <strong>{{fieldName}}</strong> from \"{{oldValue}}\" to \"{{newValue}}\" has been submitted for approval.</p>",
                "Hi {{contactName}}, your request to change {{fieldName}} from \"{{oldValue}}\" to \"{{newValue}}\" has been submitted for approval.");

        seedTemplate("SUPPLIER.CHANGE_REQUEST_DECIDED", "A decision has been made on your change request",
                "<p>Hi {{contactName}},</p><p>Your request to change <strong>{{fieldName}}</strong> has been <strong>{{decision}}</strong>.</p>",
                "Hi {{contactName}}, your request to change {{fieldName}} has been {{decision}}.");

        seedTemplate("APPROVER.NEW_REQUEST", "New supplier registration awaiting your approval",
                "<p>Hi {{approverName}},</p><p><strong>{{vendorName}}</strong> has submitted a new supplier registration awaiting your review.</p><p><a href=\"{{approveUrl}}\">Approve</a> &nbsp;|&nbsp; <a href=\"{{rejectUrl}}\">Reject</a></p><p>Or log in to the portal to review the full application first.</p>",
                "Hi {{approverName}}, {{vendorName}} has submitted a new supplier registration awaiting your review. Approve: {{approveUrl}} Reject: {{rejectUrl}}");

        seedTemplate("APPROVER.REMINDER", "Reminder: a supplier registration is awaiting your approval",
                "<p>Hi {{approverName}},</p><p>This is a reminder that <strong>{{vendorName}}</strong>'s registration has been awaiting your review since {{submittedDate}} and its SLA has now passed.</p><p><a href=\"{{approveUrl}}\">Approve</a> &nbsp;|&nbsp; <a href=\"{{rejectUrl}}\">Reject</a></p>",
                "Reminder: {{vendorName}}'s registration has been awaiting your review since {{submittedDate}} and its SLA has now passed. Approve: {{approveUrl}} Reject: {{rejectUrl}}");
    }

    private void seedTemplate(String key, String subject, String html, String plainText) {
        if (emailTemplateRepository.findByTemplateKeyAndIsActiveTrue(key).isPresent()) return;
        EmailTemplate template = new EmailTemplate();
        template.setTemplateKey(key);
        template.setSubject(subject);
        template.setHtmlBody(html);
        template.setPlainTextBody(plainText);
        template.setIsActive(true);
        emailTemplateRepository.save(template);
    }

    /** Seeds one generic, single-company approval chain for onboarding and one for change
     *  requests, each a single "Approval" stage against a shared, initially-empty "Default
     *  Approvers" group - matching the v1 single-company/generic scope. Guarded on at least
     *  one SuperAdmin existing (Workflow.superAdmin is NOT NULL) - on a genuinely fresh
     *  deployment with no admin account yet, this is a no-op and runs on the next boot after
     *  one has been created. */
    private void seedGenericWorkflows() {
        if (workflowRepository.findByWorkflowKey("VENDOR_ONBOARDING").isPresent()) return;

        List<SuperAdmin> admins = superAdminRepository.findAll(Sort.by("superAdminId"));
        if (admins.isEmpty()) {
            logger.info("No super_admin account exists yet - skipping generic workflow seed until one is created");
            return;
        }
        SuperAdmin owningAdmin = admins.get(0);

        ApproverGroup defaultApprovers = new ApproverGroup();
        defaultApprovers.setName("Default Approvers");
        defaultApprovers.setDescription("Reviews and approves new supplier registrations and change requests");
        defaultApprovers.setSuperAdmin(owningAdmin);
        defaultApprovers = approverGroupRepository.save(defaultApprovers);

        createWorkflow("VENDOR_ONBOARDING", "Vendor Onboarding",
                "Single-stage approval for new supplier registrations", owningAdmin, defaultApprovers);
        createWorkflow("VENDOR_CHANGE_REQUEST", "Vendor Change Request",
                "Single-stage approval for post-approval profile edits", owningAdmin, defaultApprovers);

        logger.info("Seeded generic VENDOR_ONBOARDING/VENDOR_CHANGE_REQUEST workflows under super_admin {}",
                owningAdmin.getSuperAdminId());
    }

    private void createWorkflow(String key, String name, String description, SuperAdmin owningAdmin, ApproverGroup approverGroup) {
        Workflow workflow = new Workflow();
        workflow.setWorkflowKey(key);
        workflow.setName(name);
        workflow.setDescription(description);
        workflow.setSuperAdmin(owningAdmin);
        workflow.setIsActive(true);

        WorkflowStage stage = new WorkflowStage();
        stage.setWorkflow(workflow);
        stage.setStageOrder(1);
        stage.setName("Approval");
        stage.setApproverGroup(approverGroup);
        stage.setSlaHours(48);
        stage.setVotingRule(VotingRule.ANY);
        stage.setRejectionBehavior(RejectionBehavior.STOP);
        stage.setIsOptional(false);
        stage.setRequiresPortalAction(false);
        workflow.getStages().add(stage);

        workflowRepository.save(workflow);
    }
}
