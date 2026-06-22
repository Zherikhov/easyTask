package com.easytask.workspace.dto;

import com.easytask.workspace.entity.WorkspaceRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddWorkspaceMemberRequest(
        @NotBlank @Email String email,
        @NotNull WorkspaceRole role,
        @Size(max = 120) String displayName
) {
}
