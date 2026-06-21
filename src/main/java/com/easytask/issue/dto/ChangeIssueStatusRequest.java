package com.easytask.issue.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ChangeIssueStatusRequest(
        @NotNull UUID statusId
) {
}
