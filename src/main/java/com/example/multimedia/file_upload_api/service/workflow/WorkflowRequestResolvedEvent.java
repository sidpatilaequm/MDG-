package com.example.multimedia.file_upload_api.service.workflow;

import com.example.multimedia.file_upload_api.entity.workflow.WorkflowRequestStatus;

/**
 * Published once a {@link com.example.multimedia.file_upload_api.entity.workflow.WorkflowRequest}
 * reaches a terminal status (APPROVED/REJECTED/ESCALATED/CANCELLED). Replaces WorkFlow's
 * cross-service outbound webhook entirely - there is no second service here to call back
 * from, so any in-process listener (e.g. SupplierOnboardingProvisioningService) just
 * subscribes to this plain Spring event instead.
 */
public class WorkflowRequestResolvedEvent {

    private final Long workflowRequestId;
    private final WorkflowRequestStatus status;

    public WorkflowRequestResolvedEvent(Long workflowRequestId, WorkflowRequestStatus status) {
        this.workflowRequestId = workflowRequestId;
        this.status = status;
    }

    public Long getWorkflowRequestId() {
        return workflowRequestId;
    }

    public WorkflowRequestStatus getStatus() {
        return status;
    }
}
