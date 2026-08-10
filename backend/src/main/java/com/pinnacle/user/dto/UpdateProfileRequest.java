package com.pinnacle.user.dto;

/** Any field left null leaves that setting unchanged. */
public record UpdateProfileRequest(
    String fullName,
    String baseCurrency,
    Boolean twoFactorEnabled,
    Boolean notificationsEnabled
) {}
