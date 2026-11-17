package com.example.multimedia.file_upload_api.entity.workflow;

/** What happens to the overall request when a stage resolves as rejected. */
public enum RejectionBehavior {
    STOP,
    RESTART,
    ESCALATE
}
