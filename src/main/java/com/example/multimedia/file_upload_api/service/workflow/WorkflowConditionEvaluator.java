package com.example.multimedia.file_upload_api.service.workflow;

import org.json.JSONObject;
import org.springframework.stereotype.Component;

/** Evaluates a {@link com.example.multimedia.file_upload_api.entity.workflow.WorkflowStage}'s
 *  conditionField/conditionOp/conditionValue (as captured in a request's frozen snapshot)
 *  against that request's requestMetadataJson. A stage with no conditionField always runs. */
@Component
public class WorkflowConditionEvaluator {

    public boolean matches(String conditionField, String conditionOp, String conditionValue, JSONObject metadata) {
        if (conditionField == null || conditionField.isBlank()) return true;

        Object actual = metadata.opt(conditionField);
        String actualStr = actual == null ? "" : String.valueOf(actual);
        String op = conditionOp == null ? "equals" : conditionOp;

        return switch (op) {
            case "equals" -> actualStr.equalsIgnoreCase(conditionValue);
            case "not_equals" -> !actualStr.equalsIgnoreCase(conditionValue);
            case "contains" -> conditionValue != null && actualStr.toLowerCase().contains(conditionValue.toLowerCase());
            case "is_true" -> Boolean.parseBoolean(actualStr);
            case "is_false" -> !Boolean.parseBoolean(actualStr);
            default -> true;
        };
    }
}
