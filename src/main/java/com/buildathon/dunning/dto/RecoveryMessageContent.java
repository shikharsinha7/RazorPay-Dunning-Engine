package com.buildathon.dunning.dto;

/**
 * A personalized recovery message the AI generated for a retry attempt.
 */
public record RecoveryMessageContent(String subject, String body) {
}
