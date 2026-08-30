package com.buildathon.dunning.entity.enums;

public enum RetryOutcome {
    PENDING,
    RECOVERED,
    FAILED,
    // A later attempt in the same strategy already recovered the payment,
    // or the payment was already terminal — this attempt never ran.
    SKIPPED
}
