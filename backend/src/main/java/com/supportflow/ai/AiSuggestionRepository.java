package com.supportflow.ai;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AiSuggestionRepository extends JpaRepository<AiSuggestion, UUID> {
}
