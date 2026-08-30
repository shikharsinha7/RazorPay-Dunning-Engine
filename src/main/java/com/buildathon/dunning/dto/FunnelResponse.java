package com.buildathon.dunning.dto;

public record FunnelResponse(
        long totalPayments,
        long failed,
        long recovered,
        long lost
) {
}
