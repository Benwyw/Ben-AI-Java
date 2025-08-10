package com.benwyw.bot.config.security;

import io.github.cdimascio.dotenv.Dotenv;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

public class JwtUtil {
    private static final Dotenv config = Dotenv.configure().ignoreIfMissing().load();
    private static final String SECRET = config.get("JWT_SECRET");
    private static final SecretKey KEY = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));

    private static final long ACCESS_TOKEN_EXPIRY =
            Long.parseLong(config.get("JWT_ACCESS_TOKEN_EXPIRY", "900000")); // ms; 15 mins
    private static final long REFRESH_TOKEN_EXPIRY =
            Long.parseLong(config.get("JWT_REFRESH_TOKEN_EXPIRY", "604800000")); // ms; 7 days

    public static String generateAccessToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRY))
                .signWith(KEY)
                .compact();
    }

    // Pair object
    public static class RefreshTokenPair {
        private final String token;
        private final String jti;
        private final OffsetDateTime expiresAt;
        public RefreshTokenPair(String token, String jti, OffsetDateTime expiresAt) {
            this.token = token; this.jti = jti; this.expiresAt = expiresAt;
        }
        public String getToken() { return token; }
        public String getJti() { return jti; }
        public OffsetDateTime getExpiresAt() { return expiresAt; }
    }

    public static RefreshTokenPair generateRefreshTokenWithJti(String username) {
        String jti = UUID.randomUUID().toString();
        long expMs = System.currentTimeMillis() + REFRESH_TOKEN_EXPIRY;
        String token = Jwts.builder()
                .subject(username)
                .id(jti) // jti
                .claim("type", "refresh")
                .issuedAt(new Date())
                .expiration(new Date(expMs))
                .signWith(KEY)
                .compact();
        return new RefreshTokenPair(token, jti,
                OffsetDateTime.ofInstant(Instant.ofEpochMilli(expMs), ZoneOffset.UTC));
    }

    public static String validateAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .clockSkewSeconds(60)        // allow ±60s drift
                    .verifyWith(KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (JwtException e) {
            return null;
        }
    }

    public static class RefreshTokenInfo {
        private final String username;
        private final String jti;
        public RefreshTokenInfo(String username, String jti) { this.username = username; this.jti = jti; }
        public String getUsername() { return username; }
        public String getJti() { return jti; }
    }

    public static RefreshTokenInfo parseRefreshToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .clockSkewSeconds(60)        // allow ±60s drift
                    .verifyWith(KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            if (!"refresh".equals(claims.get("type", String.class))) return null;
            return new RefreshTokenInfo(claims.getSubject(), claims.getId()); // jti
        } catch (JwtException e) {
            return null;
        }
    }
}
