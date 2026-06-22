package com.easytask.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AcceptInviteRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8, max = 72) String password,
        @Size(max = 120) String displayName
) {
}
