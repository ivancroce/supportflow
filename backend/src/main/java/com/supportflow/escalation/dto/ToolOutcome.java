package com.supportflow.escalation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * The result of escalating into one external tool. {@code outcome} is CREATED (newly pushed this
 * run), ALREADY_LINKED (skipped because a prior escalation already created it), or FAILED. On
 * success {@code key} and {@code url} are populated; on failure {@code error} carries the reason.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ToolOutcome(String tool, String outcome, String key, String url, String error) {

    public static ToolOutcome created(String tool, String key, String url) {
        return new ToolOutcome(tool, "CREATED", key, url, null);
    }

    public static ToolOutcome alreadyLinked(String tool, String key, String url) {
        return new ToolOutcome(tool, "ALREADY_LINKED", key, url, null);
    }

    public static ToolOutcome failed(String tool, String error) {
        return new ToolOutcome(tool, "FAILED", null, null, error);
    }

    public boolean linked() {
        return !"FAILED".equals(outcome);
    }
}
