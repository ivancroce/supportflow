package com.supportflow.ai;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Test seam for the AI advisor. Tests set the next response (or failure mode) and assert
 * how the service/controller react.
 */
@Component
@ConditionalOnProperty(name = "supportflow.ai.client", havingValue = "fake")
public class FakeGeminiClient implements GeminiClient {

    private GeminiSuggestion nextResponse = new GeminiSuggestion(
            "QUESTION", "General", "MEDIUM", "Default fake triage note.");
    private RuntimeException nextException;
    private int callCount = 0;
    private GeminiInput lastInput;

    @Override
    public GeminiSuggestion classify(GeminiInput input) {
        callCount++;
        lastInput = input;
        if (nextException != null) {
            RuntimeException toThrow = nextException;
            throw toThrow;
        }
        return nextResponse;
    }

    public void willReturn(GeminiSuggestion response) {
        this.nextResponse = response;
        this.nextException = null;
    }

    public void willThrow(RuntimeException ex) {
        this.nextException = ex;
    }

    public int callCount() {
        return callCount;
    }

    public GeminiInput lastInput() {
        return lastInput;
    }

    public void reset() {
        nextResponse = new GeminiSuggestion("QUESTION", "General", "MEDIUM", "Default fake triage note.");
        nextException = null;
        callCount = 0;
        lastInput = null;
    }
}
