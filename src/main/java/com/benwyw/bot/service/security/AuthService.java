package com.benwyw.bot.service.security;

import com.benwyw.bot.config.security.JwtUtil;
import com.benwyw.bot.data.security.User;
import com.benwyw.bot.data.security.RefreshToken;
import com.benwyw.bot.mapper.RefreshTokenMapper;
import com.benwyw.bot.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.Map;

@Service
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserMapper userMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AuthService(UserMapper userMapper, RefreshTokenMapper refreshTokenMapper) {
        this.userMapper = userMapper;
        this.refreshTokenMapper = refreshTokenMapper;
    }

    public Map<String, String> login(String username, String password) {
        User user = userMapper.findByUsername(username);
        if (user == null) {
            logger.info("Login failed: user not found {}", username);
            return null;
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            logger.info("Login failed: bad credentials for {}", username);
            return null;
        }

        String accessToken = JwtUtil.generateAccessToken(username);
        // Include a JTI in the refresh token and persist its hash
        JwtUtil.RefreshTokenPair refresh = JwtUtil.generateRefreshTokenWithJti(username);
        saveRefreshToken(user.getId(), refresh.getJti(), refresh.getToken(), refresh.getExpiresAt());

        userMapper.updateLastLogin(user.getId());
        logger.info("Login success for {}", username);
        return Map.of("accessToken", accessToken, "refreshToken", refresh.getToken());
    }

    public String refreshAccessToken(String refreshToken) {
        JwtUtil.RefreshTokenInfo info = JwtUtil.parseRefreshToken(refreshToken);
        if (info == null) return null;

        String tokenHash = sha256(refreshToken);
        int valid = refreshTokenMapper.isValid(info.getJti(), tokenHash);
        if (valid == 0) {
            logger.info("Refresh denied: token invalid or revoked (jti={})", info.getJti());
            return null;
        }
        return JwtUtil.generateAccessToken(info.getUsername());
    }

    public void logoutByRefreshToken(String refreshToken) {
        JwtUtil.RefreshTokenInfo info = JwtUtil.parseRefreshToken(refreshToken);
        if (info != null) {
            refreshTokenMapper.revokeByJti(info.getJti());
            logger.info("Refresh token revoked (jti={})", info.getJti());
        }
    }

    public void logoutAllSessions(Long userId) {
        refreshTokenMapper.revokeAllForUser(userId);
        logger.info("All refresh tokens revoked for userId={}", userId);
    }

    private void saveRefreshToken(Long userId, String jti, String rawToken, OffsetDateTime expiresAt) {
        RefreshToken rt = new RefreshToken();
        rt.setUserId(userId);
        rt.setJti(jti);
        rt.setTokenHash(sha256(rawToken));
        rt.setExpiresAt(expiresAt);
        refreshTokenMapper.insertToken(rt);
    }

    private static String sha256(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] out = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : out) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
