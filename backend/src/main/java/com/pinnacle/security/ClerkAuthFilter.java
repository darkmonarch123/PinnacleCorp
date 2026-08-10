package com.pinnacle.security;

import com.pinnacle.entity.User;
import com.pinnacle.user.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Replaces the old JwtAuthFilter (which verified tokens we issued ourselves)
 * with verification of Clerk-issued session tokens. Everything downstream —
 * every @AuthenticationPrincipal UUID userId in every controller — is
 * unchanged: this filter's job is exactly to map "a verified Clerk identity"
 * to "our internal User's UUID."
 *
 * NOTE: whether the token actually carries an email/name claim depends on a
 * JWT template being configured in the Clerk dashboard — that's a Clerk
 * account setting, not something this code controls. Without it, newly
 * auto-provisioned users get a placeholder email; see UserService.
 */
@Component
public class ClerkAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ClerkAuthFilter.class);

    private final ClerkTokenVerifier clerkTokenVerifier;
    private final UserService userService;

    public ClerkAuthFilter(ClerkTokenVerifier clerkTokenVerifier, UserService userService) {
        this.clerkTokenVerifier = clerkTokenVerifier;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                var session = clerkTokenVerifier.verify(token);
                String email = session.claims().get("email", String.class);
                String name = session.claims().get("name", String.class);

                User user = userService.findOrProvisionByClerkId(session.clerkUserId(), email, name);

                var auth = new UsernamePasswordAuthenticationToken(user.getId(), null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e) {
                log.debug("Clerk token verification failed: {}", e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}
