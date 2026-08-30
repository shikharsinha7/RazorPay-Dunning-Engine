package com.buildathon.dunning.dto;

import com.buildathon.dunning.entity.RecoveryMessage;
import com.buildathon.dunning.entity.enums.Channel;

import java.time.Instant;

public record RecoveryMessageResponse(
        Long id,
        Long retryAttemptId,
        Channel channel,
        String subject,
        String body,
        Instant sentAt,
        String provider
) {
    public static RecoveryMessageResponse from(RecoveryMessage message) {
        return new RecoveryMessageResponse(
                message.getId(),
                message.getRetryAttempt().getId(),
                message.getChannel(),
                message.getSubject(),
                message.getBody(),
                message.getSentAt(),
                message.getProvider()
        );
    }
}
