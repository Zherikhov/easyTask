package com.easytask.issue.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record IssueCommentResponse(
        UUID id,
        UUID issueId,
        UUID authorId,
        String authorName,
        String body,
        OffsetDateTime editedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
