package com.supportflow.ai;

import com.supportflow.ai.dto.AiSuggestionEnvelope;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets/{id}/ai-suggestion")
public class AiSuggestionController {

    private final AiAdvisorService aiAdvisorService;

    public AiSuggestionController(AiAdvisorService aiAdvisorService) {
        this.aiAdvisorService = aiAdvisorService;
    }

    @GetMapping
    public AiSuggestionEnvelope get(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        return aiAdvisorService.getSuggestion(userId, id);
    }

    @PostMapping("/regenerate")
    public AiSuggestionEnvelope regenerate(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        return aiAdvisorService.regenerate(userId, id);
    }
}
