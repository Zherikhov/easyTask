package com.easytask.project.dto;

import com.easytask.project.entity.ProjectRole;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProjectResponse(
        UUID id,
        UUID workspaceId,
        String key,
        String name,
        String description,
        ProjectRole myRole,
        OffsetDateTime createdAt
) {
}
