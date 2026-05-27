package com.supportflow.ai.dto;

import com.supportflow.ai.AiSuggestion;
import java.time.Instant;

public record AiSuggestionResponse(
        String suggestedType,
        String suggestedCategory,
        String suggestedPriority,
        String draftNote,
        Instant generatedAt) {

    public static AiSuggestionResponse from(AiSuggestion s) {
        return new AiSuggestionResponse(
                s.getSuggestedType(),
                s.getSuggestedCategory(),
                s.getSuggestedPriority(),
                s.getDraftNote(),
                s.getGeneratedAt());
    }
}
