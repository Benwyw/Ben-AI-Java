package com.dahsing.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class JwtTokenUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenUtil.class);
    private static final Set<String> allowedAuthorities = new HashSet<>();

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.issuer:your-application-name}")
    private String issuer;

    private Key signingKey;

    @PostConstruct
    public void init() {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
        allowedAuthorities.add("generateReport");
        allowedAuthorities.add("encryption");
        allowedAuthorities.add("idv");
        allowedAuthorities.add("autoWorkflow");
    }

    /**
     * Generate token for a user
     *
     * @param userDetails User information
     * @return JWT token string
     */
    public String generateToken(UserDetails userDetails, Duration accessTtl) {
        Map<String, Object> claims = new HashMap<>();
        List<String> authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(allowedAuthorities::contains)
                .collect(Collectors.toList());
        claims.put("authorities", authorities);
        return createToken(claims, userDetails.getUsername(), accessTtl);
    }

    /**
     * Creates a token with claims and subject
     */
    private String createToken(Map<String, Object> claims, String subject, Duration ttl) {
        Instant now = Instant.now();
        Instant exp = now.plus(ttl);
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .setIssuer(issuer)
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }


    /**
     * Generate a refresh token for the user
     *
     * @param username Username of the user
     * @param refreshTtl Duration for which the refresh token is valid
     * @return JWT refresh token string
     */
    public String generateRefreshToken(String username, Duration refreshTtl) {
        Instant now = Instant.now();
        Instant expiry = now.plus(refreshTtl);

        return Jwts.builder()
                .setSubject(username)
                .setIssuer(issuer)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(expiry))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extract username from token
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extract expiration date from token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extract a specific claim from the token
     */
    public <T> T extractClaim(String token, java.util.function.Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract all claims from token
     */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Extract authorities/permissions from token
     */
    @SuppressWarnings("unchecked")
    public List<String> extractAuthorities(String token) {
        Claims claims = extractAllClaims(token);
        return (List<String>) claims.get("authorities");
    }

    /**
     * Check if token has expired
     */
    private Boolean isTokenExpired(String token) {
        final Date expiration = extractExpiration(token);
        return expiration.before(new Date());
    }

    /**
     * Validate token
     */
    public Boolean validateToken(String token) {
        try {
            // Check if token is well-formed, has valid signature, and is not expired
            Jwts.parser()
                    .verifyWith((SecretKey) signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            logger.error("JWT token is expired: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            logger.error("JWT token is unsupported: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            logger.error("Invalid JWT token: {}", ex.getMessage());
        } catch (SecurityException ex) {
            logger.error("Invalid JWT signature: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            logger.error("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Calculate remaining validity time in seconds
     */
    public long getRemainingValidityTime(String token) {
        final Date expiration = extractExpiration(token);
        final Date now = new Date();
        return (expiration.getTime() - now.getTime()) / 1000;
    }

    /**
     * Generate a refreshed token (with new expiration)
     */
    public String refreshToken(String token, Duration ttl) {
        final Claims claims = extractAllClaims(token);
        return createToken(claims, claims.getSubject(), ttl);
    }
}
