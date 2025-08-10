package com.benwyw.bot.controller.web;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class SecurityExceptionHandler {
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<Map<String,String>> handleExpired(ExpiredJwtException e) {
        return ResponseEntity.status(401).body(Map.of("code","ACCESS_TOKEN_EXPIRED","message","Access token expired"));
    }
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<Map<String,String>> handleJwt(JwtException e) {
        return ResponseEntity.status(401).body(Map.of("code","ACCESS_TOKEN_INVALID","message","Invalid access token"));
    }
}
