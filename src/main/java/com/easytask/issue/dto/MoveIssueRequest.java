package com.easytask.issue.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record MoveIssueRequest(
        @NotNull UUID statusId,
        UUID prevIssueId,
        UUID nextIssueId
) {
}
