package com.benwyw.bot.controller.web;

import com.benwyw.bot.data.security.User;
import com.benwyw.bot.mapper.UserMapper;
import com.benwyw.bot.service.security.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final UserMapper userMapper;

    public AuthController(AuthService authService, UserMapper userMapper) {
        this.authService = authService;
        this.userMapper = userMapper;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        Map<String, String> tokens = authService.login(username, password);
        if (tokens == null) {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        Map<String, String> tokens = authService.refreshAccessToken(refreshToken);
        if (tokens == null) {
            return ResponseEntity.status(401).body("Invalid refresh token");
        }
        // Returns both new accessToken and new refreshToken to extend/prolong the session
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        authService.logoutByRefreshToken(refreshToken);
        return ResponseEntity.ok().build();
    }

    // Optional: logout all sessions (requires user context)
    @PostMapping("/logout-all")
    public ResponseEntity<?> logoutAll(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        User u = userMapper.findByUsername(username);
        if (u != null) {
            authService.logoutAllSessions(u.getId());
        }
        return ResponseEntity.ok().build();
    }
}
