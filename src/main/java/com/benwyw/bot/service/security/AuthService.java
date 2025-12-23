package com.benwyw.bot.service.security;

import com.benwyw.bot.config.security.JwtUtil;
import com.benwyw.bot.data.security.RefreshToken;
import com.benwyw.bot.data.security.User;
import com.benwyw.bot.mapper.RefreshTokenMapper;
import com.benwyw.bot.mapper.UserMapper;
import com.benwyw.util.embeds.EmbedColor;
import com.benwyw.util.embeds.EmbedUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Objects;

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

        String accessToken = JwtUtil.generateAccessToken(username, user.getRole());
        // Include a JTI in the refresh token and persist its hash
        JwtUtil.RefreshTokenPair refresh = JwtUtil.generateRefreshTokenWithJti(username);
        saveRefreshToken(user.getId(), refresh.getJti(), refresh.getToken(), refresh.getExpiresAt());

        userMapper.updateLastLogin(user.getId());
        logger.info("Login success for {}", username);
        return Map.of("accessToken", accessToken, "refreshToken", refresh.getToken());
    }

    /**
     * Refresh tokens and extend session.
     * Returns both a new access token AND a new refresh token to prolong the session.
     */
    public Map<String, String> refreshAccessToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            logger.info("Refresh denied: empty or null refresh token");
            return null;
        }

        // Trim any whitespace that might have been added by frontend
        refreshToken = refreshToken.trim();

        JwtUtil.RefreshTokenInfo info = JwtUtil.parseRefreshToken(refreshToken);
        if (info == null) {
            logger.info("Refresh denied: failed to parse refresh token");
            return null;
        }

        String tokenHash = sha256(refreshToken);
        logger.debug("Refresh attempt: jti={}, tokenHash={}", info.getJti(), tokenHash);

        int valid = refreshTokenMapper.isValid(info.getJti(), tokenHash);
        if (valid == 0) {
            logger.info("Refresh denied: token invalid or revoked (jti={}, computedHash={})", info.getJti(), tokenHash);
            return null;
        }

        String username = info.getUsername();
        User user = userMapper.findByUsername(username);
        if (user == null) {
            logger.info("Refresh denied: user not found {}", username);
            return null;
        }

        // Revoke the old refresh token
        refreshTokenMapper.revokeByJti(info.getJti());

        // Generate new access token with user role
        String newAccessToken = JwtUtil.generateAccessToken(username, user.getRole());

        // Generate new refresh token to extend/prolong the session
        JwtUtil.RefreshTokenPair newRefresh = JwtUtil.generateRefreshTokenWithJti(username);
        saveRefreshToken(user.getId(), newRefresh.getJti(), newRefresh.getToken(), newRefresh.getExpiresAt());

        logger.info("Refresh success for {} (old jti={}, new jti={})", username, info.getJti(), newRefresh.getJti());
        return Map.of("accessToken", newAccessToken, "refreshToken", newRefresh.getToken());
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
        String hash = sha256(rawToken);
        rt.setTokenHash(hash);
        rt.setExpiresAt(expiresAt.toLocalDateTime()); // convert here
        refreshTokenMapper.insertToken(rt);
        logger.debug("Saved refresh token: jti={}, tokenHash={}, expiresAt={}", jti, hash, expiresAt);
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

    @Transactional
    @CacheEvict(cacheNames = "userBaseCache", allEntries = true)
    public MessageEmbed insertUserFromEvent(SlashCommandInteractionEvent event) {
        MessageEmbed masterMessageEmbed = EmbedUtils.createSuccess("Operation started.");
        try {
            String username = event.getOption("username").getAsString();
            String rawPassword = event.getOption("password").getAsString();
            String email = getOptionValue(event, "email");
            String role = getOptionValue(event, "role");
            String status = getOptionValue(event, "status");
            String remarks = getOptionValue(event, "remarks");

            User u = new User();
            u.setUsername(username.trim());
            u.setPasswordHash(passwordEncoder.encode(rawPassword));
            u.setEmail(StringUtils.trimToNull(email));
            u.setRole(StringUtils.defaultIfBlank(role, "USER").toUpperCase());
            u.setStatus(StringUtils.defaultIfBlank(status, "ACTIVE").toUpperCase());
            u.setCreatedAt(OffsetDateTime.now());
            u.setUpdatedAt(OffsetDateTime.now());

            int count = userMapper.insertUser(u);

            // Build embed just like your weight() method
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle("User Management", "https://your-docs-url.example/user"); // adjust URL
            embedBuilder.setDescription(count > 0 ? "INSERT successful" : "INSERT failure");
            embedBuilder.setAuthor("AuthService");
            embedBuilder.setFooter(String.valueOf(LocalDateTime.now(ZoneId.of("Asia/Hong_Kong"))));
            embedBuilder.setThumbnail("https://i.imgur.com/b81zA3M.png"); // your own image here

            // Show user fields like in SELECT mode from your weight() example
            embedBuilder.addField("ID", u.getId() != null ? u.getId().toString() : "", true);
            embedBuilder.addField("Username", u.getUsername(), true);
            if (StringUtils.isNotBlank(u.getEmail())) {
                embedBuilder.addField("Email", u.getEmail(), true);
            }
            embedBuilder.addField("Role", u.getRole(), true);
            embedBuilder.addField("Status", u.getStatus(), true);
            if (StringUtils.isNotBlank(remarks)) {
                embedBuilder.addField("Remarks", remarks, true);
            }

            embedBuilder.setColor(count > 0 ? EmbedColor.SUCCESS.color : EmbedColor.ERROR.color);
            masterMessageEmbed = embedBuilder.build();

        } catch (Exception e) {
            masterMessageEmbed = EmbedUtils.createError(String.format("Operation failed.\n%s", e.getMessage()));
        }
        return masterMessageEmbed;
    }

    private String getOptionValue(SlashCommandInteractionEvent event, String name) {
        OptionMapping opt = event.getOption(name);
        return opt != null ? opt.getAsString() : null;
    }

    @Transactional
    public MessageEmbed purgeRefreshTokensFromEvent(SlashCommandInteractionEvent event) {
        boolean dryRun = event.getOption("dryrun") != null && event.getOption("dryrun").getAsBoolean();
        int affected = dryRun ? refreshTokenMapper.countExpiredOrRevoked()
                : refreshTokenMapper.purgeExpiredOrRevoked();

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("Refresh Token Maintenance", "https://your-docs-url.example/tokens");
        eb.setDescription(dryRun ? "DRY-RUN completed" : "PURGE executed");
        eb.setAuthor("AuthService");
        eb.setFooter(String.valueOf(LocalDateTime.now(ZoneId.of("Asia/Hong_Kong"))));
        eb.setThumbnail("https://i.imgur.com/b81zA3M.png");
        eb.addField(dryRun ? "Rows that would be deleted" : "Rows deleted", String.valueOf(affected), true);
        eb.setColor(EmbedColor.SUCCESS.color);
        return eb.build();
    }

    @Transactional
    public MessageEmbed deleteUserFromEvent(SlashCommandInteractionEvent event) {
        String username = Objects.requireNonNull(event.getOption("username")).getAsString();

        // delete tokens first
        User u = userMapper.findByUsername(username);
        if (u != null) {
            refreshTokenMapper.deleteTokensByUserId(u.getId());
        }
        // then delete user
        int affected = userMapper.deleteUserByUsername(username);

        EmbedBuilder eb = new EmbedBuilder();
        eb.setTitle("User Maintenance", "https://your-docs-url.example/users");
        eb.setDescription("User deletion executed");
        eb.setAuthor("AuthService");
        eb.setFooter(String.valueOf(LocalDateTime.now(ZoneId.of("Asia/Hong_Kong"))));
        eb.setThumbnail("https://i.imgur.com/b81zA3M.png");
        eb.addField("Rows deleted", String.valueOf(affected), true);
        eb.setColor(EmbedColor.SUCCESS.color);
        return eb.build();
    }

}
