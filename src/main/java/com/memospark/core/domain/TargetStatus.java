package com.memospark.core.domain;

public enum TargetStatus {
    PREPARING,
    APPLIED,
    WRITTEN_TEST,
    INTERVIEW_1,
    INTERVIEW_2,
    HR,
    OFFER,
    REJECTED,
    // Legacy values kept so existing rows remain readable.
    INTERVIEWING,
    CLOSED
}
