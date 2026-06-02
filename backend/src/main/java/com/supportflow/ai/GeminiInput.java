package com.supportflow.ai;

public record GeminiInput(
        String ticketSubject,
        String ticketDescription,
        String projectName,
        String projectClientName,
        String projectDescription) {
}
