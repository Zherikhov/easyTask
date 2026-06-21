package com.easytask.issue.dto;

import com.easytask.issue.entity.IssuePriority;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record IssueResponse(
        UUID id,
        UUID projectId,
        String key,
        long number,
        String title,
        String description,
        UUID projectIssueTypeId,
        String issueTypeName,
        UUID statusId,
        String statusName,
        IssuePriority priority,
        UUID reporterId,
        UUID assigneeId,
        BigDecimal position,
        LocalDate dueDate,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        int version
) {
}
