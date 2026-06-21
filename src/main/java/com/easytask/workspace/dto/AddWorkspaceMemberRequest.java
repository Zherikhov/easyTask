package com.easytask.workspace.dto;

import com.easytask.workspace.entity.WorkspaceRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddWorkspaceMemberRequest(
        @NotBlank @Email String email,
        @NotNull WorkspaceRole role
) {
}
