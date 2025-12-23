package com.dahsing.controller.security;

import com.dahsing.model.constant.ModuleEnum;
import com.dahsing.model.properties.JwtProps;
import com.dahsing.service.security.*;
import com.dahsing.util.IpUtil;
import com.dahsing.util.JwtTokenUtil;
import com.dahsing.util.LogUtil;
import io.jsonwebtoken.JwtException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationService authService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private ActivityRegistry activityRegistry;

    @Autowired
    private InMemoryUserRepository userRepo;

    @Autowired
    private JwtProps props;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String uid,
                                   @RequestParam String password,
                                   HttpServletRequest request) {
        String msgSeq = "msgSeq";
        try {
            if (StringUtils.isBlank(uid) || StringUtils.isBlank(password)) {
                LogUtil.info(msgSeq, ModuleEnum.APP, String.format("Login rejected: missing parameters ip=%s ua=%s", clientIp(request), userAgent(request)));
                Map<String, String> body = new HashMap<>();
                body.put("error", "Missing login parameters");
                return ResponseEntity.badRequest().body(body);
            }

            UserDetails userDetails = authService.authenticateWithCredential(uid, password, msgSeq);
            activityRegistry.update(userDetails.getUsername());
            userRepo.saveOrUpdate(userDetails); // ensure refresh can rebuild authorities

            String token = jwtTokenUtil.generateToken(userDetails, props.getAccessTtl());

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("username", userDetails.getUsername());
            response.put("authorities", userDetails.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority).collect(Collectors.toList()));

            String refreshToken = jwtTokenUtil.generateRefreshToken(userDetails.getUsername(), props.getRefreshTtl());

            String contextPath = request.getContextPath();
            boolean secureCookie = request.isSecure(); // or decide by profile
            ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                    .httpOnly(true)
                    .secure(secureCookie)
                    .sameSite("Strict")
                    .path(contextPath + "/auth")
                    .maxAge(props.getRefreshTtl())
                    .build();

            LogUtil.info(msgSeq, ModuleEnum.APP, String.format("Login success user=%s ip=%s ua=%s",
                    userDetails.getUsername(), clientIp(request), userAgent(request)));

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(response);

        } catch (Exception e) {
            // do NOT log credentials or tokens
            LogUtil.info(msgSeq, ModuleEnum.APP, String.format("Login failed uid=%s ip=%s reason=%s", uid, clientIp(request), e.getMessage()));
            return ResponseEntity.status(401).body("Authentication failed");
        }
    }

    @GetMapping("/login")
    public ResponseEntity<?> loginMethodNotAllowed() {
        Map<String, Object> response = new HashMap<>();
        response.put("error", "Method Not Allowed");
        response.put("message", "Authentication requires POST method");

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@CookieValue(value = "refreshToken", required = false) String refreshToken,
                                          HttpServletRequest request) {
        String msgSeq = "msgSeq";
        if (refreshToken == null) {
            LogUtil.info(msgSeq, ModuleEnum.APP, String.format("Refresh rejected: no refresh token ip=%s ua=%s", clientIp(request), userAgent(request)));
            Map<String, String> body = new HashMap<>();
            body.put("error", "No refresh token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(body);
        }
        try {
            String username = jwtTokenUtil.extractUsername(refreshToken);

            if (activityRegistry.isExpired(username, props.getRefreshIdleTtl())) {
                activityRegistry.clear(username);
                userRepo.remove(username); // keep stores consistent
                LogUtil.info(msgSeq, ModuleEnum.APP, String.format("Session idle-expired user=%s ip=%s thresholdMs=%s",
                        username, clientIp(request), props.getRefreshIdleTtl()));
                Map<String, String> body = new HashMap<>();
                body.put("error", "Idle timeout — please re-login");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(body);
            }

            UserDetails userDetails = authService.loadUserByUsername(username);
            String newAccessToken = jwtTokenUtil.generateToken(userDetails, props.getAccessTtl());
            activityRegistry.update(username);
            if (userDetails != null) {
                userRepo.updateLastActive(username); // if you expose this method
            }

            // 👉 Reissue refresh cookie here
            String newRefreshToken = jwtTokenUtil.generateRefreshToken(username, props.getRefreshTtl()); // this will always mint new refresh tokens with, otherwise: refreshToken

            String contextPath = request.getContextPath();
            boolean secureCookie = request.isSecure();

            ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", newRefreshToken)
                    .httpOnly(true)
                    .secure(secureCookie)
                    .sameSite("Strict")
                    .path(contextPath + "/auth")
                    .maxAge(props.getRefreshTtl()) // or .maxAge(props.getRefreshTtl().getSeconds()) if Duration overload unavailable
                    .build();

            LogUtil.info(msgSeq, ModuleEnum.APP, String.format("Refresh success user=%s ip=%s", username, clientIp(request)));
            Map<String, String> body = new HashMap<>();
            body.put("token", newAccessToken);
            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .body(body);
        } catch (JwtException ex) {
            LogUtil.info(msgSeq, ModuleEnum.APP, String.format("Refresh rejected: invalid token ip=%s reason=%s", clientIp(request), ex.getMessage()));
            Map<String, String> body = new HashMap<>();
            body.put("error", "Invalid refresh token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(body);
        } catch (UsernameNotFoundException ex) {
            LogUtil.info(msgSeq, ModuleEnum.APP, String.format("Refresh rejected: user not found user=%s ip=%s", ex.getMessage(), clientIp(request)));
            Map<String, String> body = new HashMap<>();
            body.put("error", "User not found");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(body);
        } catch (Exception ex) {
            LogUtil.warn(msgSeq, ModuleEnum.APP, String.format("Refresh error ip=%s reason=%s", clientIp(request), ex));
            Map<String, String> body = new HashMap<>();
            body.put("error", "Server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(body);
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestParam String uid, HttpServletRequest request) {
        String msgSeq = "msgSeq";

        // if you keep username client-side; alternatively derive from JWT subject if provided
        activityRegistry.clear(uid);
        userRepo.remove(uid);

        String contextPath = request.getContextPath();
        ResponseCookie expired = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(request.isSecure())
                .sameSite("Strict")
                .path(contextPath + "/auth")
                .maxAge(0)
                .build();

        LogUtil.info(msgSeq, ModuleEnum.APP, String.format("Logout success user=%s ip=%s", uid, clientIp(request)));
        Map<String, String> body = new HashMap<>();
        body.put("status", "logged out");
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expired.toString())
                .body(body);
    }

    private static String clientIp(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !StringUtils.isBlank(xff)) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }

    private static String userAgent(HttpServletRequest req) {
        return Optional.ofNullable(req.getHeader("User-Agent")).orElse("-");
    }

    @GetMapping("/get-ip-username")
    public ResponseEntity<String> getUsername(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.OK).body(IpUtil.getClientIpAddr(request));
    }

}
