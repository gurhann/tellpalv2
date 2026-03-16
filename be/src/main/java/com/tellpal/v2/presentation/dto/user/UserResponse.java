package com.tellpal.v2.presentation.dto.user;

public record UserResponse(Long id, String firebaseUid, boolean isAllowMarketing) {
}
