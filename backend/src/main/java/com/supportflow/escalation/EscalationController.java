package com.supportflow.escalation;

import com.supportflow.escalation.dto.EscalationResult;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets/{id}/escalate")
public class EscalationController {

    private final EscalationService escalationService;

    public EscalationController(EscalationService escalationService) {
        this.escalationService = escalationService;
    }

    @PostMapping
    public EscalationResult escalate(@AuthenticationPrincipal UUID userId, @PathVariable UUID id) {
        return escalationService.escalate(userId, id);
    }
}
