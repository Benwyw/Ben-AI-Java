package com.benwyw.bot.controller.web;

import com.benwyw.bot.config.security.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private static final String HARDCODED_USERNAME = "testuser";
    private static final String HARDCODED_PASSWORD = "testpass";

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        if (HARDCODED_USERNAME.equals(username) && HARDCODED_PASSWORD.equals(password)) {
            String accessToken = JwtUtil.generateAccessToken(username);
            String refreshToken = JwtUtil.generateRefreshToken(username);
            Map<String, String> tokens = new HashMap<>();
            tokens.put("accessToken", accessToken);
            tokens.put("refreshToken", refreshToken);
            return ResponseEntity.ok(tokens);
        }
        return ResponseEntity.status(401).body("Invalid credentials");
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        String username = JwtUtil.validateRefreshToken(refreshToken);
        if (username != null) {
            String accessToken = JwtUtil.generateAccessToken(username);
            Map<String, String> tokens = new HashMap<>();
            tokens.put("accessToken", accessToken);
            return ResponseEntity.ok(tokens);
        }
        return ResponseEntity.status(401).body("Invalid refresh token");
    }
}

