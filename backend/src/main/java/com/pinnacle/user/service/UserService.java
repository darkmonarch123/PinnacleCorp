package com.pinnacle.user.service;

import com.pinnacle.entity.Account;
import com.pinnacle.entity.User;
import com.pinnacle.repository.AccountRepository;
import com.pinnacle.repository.UserRepository;
import com.pinnacle.user.dto.ProfileResponse;
import com.pinnacle.user.dto.UpdateProfileRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final BigDecimal startingBalance;

    public UserService(
            UserRepository userRepository,
            AccountRepository accountRepository,
            @Value("${pinnacle.demo.starting-balance}") BigDecimal startingBalance
    ) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.startingBalance = startingBalance;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(UUID userId) {
        return ProfileResponse.from(findUser(userId));
    }

    @Transactional
    public ProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        User user = findUser(userId);

        if (request.fullName() != null) user.setFullName(request.fullName());
        if (request.baseCurrency() != null) user.setBaseCurrency(request.baseCurrency());
        if (request.twoFactorEnabled() != null) user.setTwoFactorEnabled(request.twoFactorEnabled());
        if (request.notificationsEnabled() != null) user.setNotificationsEnabled(request.notificationsEnabled());

        userRepository.save(user);
        return ProfileResponse.from(user);
    }

    @Transactional
    public void completeKyc(UUID userId, String fullName, LocalDate dateOfBirth, String country) {
        User user = findUser(userId);
        user.setFullName(fullName);
        user.setDateOfBirth(dateOfBirth);
        user.setCountry(country);
        user.setKycCompleted(true);
        userRepository.save(user);
    }

    /**
     * Called from ClerkAuthFilter on every request. Clerk owns sign-up, so
     * there's no separate "register" endpoint anymore — the first request
     * from a new Clerk identity is what creates our User + demo Account.
     */
    @Transactional
    public User findOrProvisionByClerkId(String clerkUserId, String email, String fullNameGuess) {
        return userRepository.findByClerkUserId(clerkUserId).orElseGet(() -> {
            User user = new User();
            user.setClerkUserId(clerkUserId);
            user.setEmail(email != null ? email : clerkUserId + "@clerk.placeholder");
            user.setFullName(fullNameGuess != null && !fullNameGuess.isBlank() ? fullNameGuess : "New User");
            user.setEmailVerified(true); // Clerk already verified it before issuing a session
            userRepository.save(user);

            Account account = new Account();
            account.setUserId(user.getId());
            account.setBalance(startingBalance);
            account.setBuyingPower(startingBalance);
            accountRepository.save(account);

            return user;
        });
    }

    private User findUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));
    }
}
