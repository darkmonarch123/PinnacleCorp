package com.pinnacle.security;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.security.interfaces.RSAPublicKey;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.net.MalformedURLException;

/**
 * Clerk signs its session tokens with RS256, rotating keys under a JWKS
 * endpoint derived from the issuer — unlike our old JwtService, there's no
 * shared secret here, just Clerk's public keys. Keys are cached per `kid`
 * (JWKS rotates rarely) so this doesn't hit the network on every request.
 */
@Service
public class ClerkTokenVerifier {

    private final JwkProvider jwkProvider;
    private final String issuer;
    private final Map<String, RSAPublicKey> keyCache = new ConcurrentHashMap<>();

    public ClerkTokenVerifier(@Value("${pinnacle.clerk.issuer}") String issuer) throws MalformedURLException {
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalStateException(
                "pinnacle.clerk.issuer (CLERK_ISSUER) is not set — decode your Clerk publishable key " +
                "(base64, minus the pk_test_/pk_live_ prefix and trailing $) to find it, " +
                "e.g. https://your-app.clerk.accounts.dev"
            );
        }
        this.issuer = issuer.replaceAll("/+$", "");
        try{
             this.jwkProvider = new UrlJwkProvider(URI.create(this.issuer + "/.well-known/jwks.json").toURL());
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Invalid Clerk issuer URL", e);
        }
    }

    public record VerifiedSession(String clerkUserId, Claims claims) {}

    /** @throws io.jsonwebtoken.JwtException (or subtypes) if the token is invalid, expired, or from a different issuer */
    public VerifiedSession verify(String token) {
        String kid = extractKid(token);
        RSAPublicKey key = keyCache.computeIfAbsent(kid, this::fetchKey);

        Jws<Claims> parsed = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token);

        return new VerifiedSession(parsed.getPayload().getSubject(), parsed.getPayload());
    }

    private RSAPublicKey fetchKey(String kid) {
        try {
            Jwk jwk = jwkProvider.get(kid);
            return (RSAPublicKey) jwk.getPublicKey();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to fetch Clerk JWKS key for kid=" + kid, e);
        }
    }

    private String extractKid(String token) {
        String[] parts = token.split("\\.");
        if (parts.length < 2) throw new IllegalArgumentException("Malformed JWT");
        String headerJson = new String(java.util.Base64.getUrlDecoder().decode(parts[0]));
        var matcher = java.util.regex.Pattern.compile("\"kid\"\\s*:\\s*\"([^\"]+)\"").matcher(headerJson);
        if (!matcher.find()) throw new IllegalArgumentException("JWT header missing kid");
        return matcher.group(1);
    }
}
