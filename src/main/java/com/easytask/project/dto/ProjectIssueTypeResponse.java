package com.easytask.project.dto;

import java.util.UUID;

public record ProjectIssueTypeResponse(
        UUID id,
        String name,
        int position
) {
}
