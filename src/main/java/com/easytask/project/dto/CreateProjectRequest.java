package com.easytask.project.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectRequest(
        @NotBlank @Size(max = 10) String key,
        @NotBlank @Size(max = 120) String name,
        String description
) {
}
