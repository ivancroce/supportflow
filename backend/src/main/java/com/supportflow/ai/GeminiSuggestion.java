package com.supportflow.ai;

public record GeminiSuggestion(
        String suggestedType,
        String suggestedCategory,
        String suggestedPriority,
        String draftNote) {
}
