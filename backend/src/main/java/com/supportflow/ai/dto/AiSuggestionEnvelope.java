package com.supportflow.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AiSuggestionEnvelope(String status, AiSuggestionResponse suggestion, String error) {

    public static AiSuggestionEnvelope ready(AiSuggestionResponse suggestion) {
        return new AiSuggestionEnvelope("READY", suggestion, null);
    }

    public static AiSuggestionEnvelope failed(String error) {
        return new AiSuggestionEnvelope("FAILED", null, error);
    }
}
