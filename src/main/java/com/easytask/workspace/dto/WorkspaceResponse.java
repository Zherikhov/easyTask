package com.easytask.workspace.dto;

import com.easytask.workspace.entity.WorkspaceRole;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WorkspaceResponse(
        UUID id,
        String name,
        String slug,
        UUID ownerId,
        WorkspaceRole myRole,
        OffsetDateTime createdAt
) {
}
