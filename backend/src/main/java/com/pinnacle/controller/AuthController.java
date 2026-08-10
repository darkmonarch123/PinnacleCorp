package com.pinnacle.controller;

import com.pinnacle.dto.KycRequest;
import com.pinnacle.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Register/login are gone — Clerk's own hosted components handle those
 * directly. The mock KYC step is the one auth-adjacent thing still ours.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/kyc")
    public ResponseEntity<Void> submitKyc(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody KycRequest request
    ) {
        userService.completeKyc(userId, request.fullName(), request.dateOfBirth(), request.country());
        return ResponseEntity.noContent().build();
    }
}
