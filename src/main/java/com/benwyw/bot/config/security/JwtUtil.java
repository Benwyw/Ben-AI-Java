package com.benwyw.bot.config.security;

import io.github.cdimascio.dotenv.Dotenv;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.util.Date;

public class JwtUtil {
    private static final Dotenv config = Dotenv.configure().ignoreIfMissing().load();

    // Example: a Base64-encoded 256-bit key (generated once and stored securely in .env)
    // Generate your own: Jwts.SIG.HS256.key().build() then Base64 encode it
    private static final String SECRET = config.get("JWT_SECRET");

    private static final SecretKey KEY =
            Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET));

    private static final long ACCESS_TOKEN_EXPIRY =
            Long.parseLong(config.get("JWT_ACCESS_TOKEN_EXPIRY", "900000"));

    private static final long REFRESH_TOKEN_EXPIRY =
            Long.parseLong(config.get("JWT_REFRESH_TOKEN_EXPIRY", "604800000"));

    public static String generateAccessToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRY))
                .signWith(KEY)
                .compact();
    }

    public static String generateRefreshToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRY))
                .claim("type", "refresh")
                .signWith(KEY)
                .compact();
    }

    public static String validateAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.getSubject();
        } catch (JwtException e) {
            return null;
        }
    }

    public static String validateRefreshToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String type = claims.get("type", String.class);
            return "refresh".equals(type) ? claims.getSubject() : null;
        } catch (JwtException e) {
            return null;
        }
    }
}
