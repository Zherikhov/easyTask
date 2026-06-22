package com.easytask.project.dto;

import com.easytask.auth.entity.UserStatus;
import com.easytask.project.entity.ProjectRole;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProjectMemberResponse(
        UUID userId,
        String email,
        String displayName,
        ProjectRole role,
        UserStatus status,
        OffsetDateTime createdAt
) {
}
