package com.pinnacle.user.dto;

import com.pinnacle.entity.User;

import java.util.UUID;

public record ProfileResponse(
    UUID id,
    String email,
    String fullName,
    String country,
    String baseCurrency,
    boolean twoFactorEnabled,
    boolean notificationsEnabled,
    boolean kycCompleted,
    boolean emailVerified
) {
    public static ProfileResponse from(User user) {
        return new ProfileResponse(
            user.getId(), user.getEmail(), user.getFullName(), user.getCountry(), user.getBaseCurrency(),
            user.isTwoFactorEnabled(), user.isNotificationsEnabled(), user.isKycCompleted(), user.isEmailVerified()
        );
    }
}
