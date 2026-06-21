package com.easytask.project.dto;

import com.easytask.project.entity.ProjectRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddProjectMemberRequest(
        @NotBlank @Email String email,
        @NotNull ProjectRole role
) {
}
