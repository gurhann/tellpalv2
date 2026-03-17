package com.tellpal.v2.admin.web.admin;

import jakarta.validation.constraints.NotBlank;

public record AdminRefreshRequest(
        @NotBlank(message = "refreshToken is required")
        String refreshToken) {
}
