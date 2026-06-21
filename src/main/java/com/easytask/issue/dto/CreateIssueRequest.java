package com.easytask.issue.dto;

import com.easytask.issue.entity.IssuePriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreateIssueRequest(
        @NotNull UUID projectIssueTypeId,
        @NotBlank @Size(max = 255) String title,
        String description,
        IssuePriority priority,
        UUID assigneeId,
        LocalDate dueDate
) {
}
