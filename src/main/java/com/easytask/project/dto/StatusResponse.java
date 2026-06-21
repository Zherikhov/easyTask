package com.easytask.project.dto;

import com.easytask.project.entity.StatusCategory;

import java.util.UUID;

public record StatusResponse(
        UUID id,
        String name,
        StatusCategory category,
        int position
) {
}
