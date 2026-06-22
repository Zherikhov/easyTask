package com.easytask.workspace.dto;

import com.easytask.auth.entity.UserStatus;
import com.easytask.workspace.entity.WorkspaceRole;

import java.time.OffsetDateTime;
import java.util.UUID;

public record WorkspaceMemberResponse(
        UUID userId,
        String email,
        String displayName,
        WorkspaceRole role,
        UserStatus status,
        OffsetDateTime createdAt,
        String inviteToken
) {
}
