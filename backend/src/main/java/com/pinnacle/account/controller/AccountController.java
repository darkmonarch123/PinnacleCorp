package com.pinnacle.account.controller;

import com.pinnacle.account.dto.AccountSummaryResponse;
import com.pinnacle.account.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public ResponseEntity<AccountSummaryResponse> getSummary(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(accountService.getSummary(userId));
    }

    @PostMapping("/reset")
    public ResponseEntity<AccountSummaryResponse> resetDemoBalance(@AuthenticationPrincipal UUID userId) {
        accountService.resetDemoBalance(userId);
        return ResponseEntity.ok(accountService.getSummary(userId));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleClientError(IllegalStateException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", e.getMessage()));
    }
}
