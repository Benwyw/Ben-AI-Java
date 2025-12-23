package com.benwyw.bot.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;

import java.io.IOException;

@Slf4j
@Configuration
@Profile("!local")
public class ForwardedHeaderFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String referer = httpRequest.getHeader("referer");
//        log.info(String.format("referer: %s", referer));
        String remoteAddr = request.getRemoteAddr();
//        log.info(String.format("remoteAddr: %s", remoteAddr));
//        String forwardedFor = httpRequest.getHeader("X-Forwarded-For");
//        log.info(String.format("forwardedFor: %s", forwardedFor));
//        if (forwardedFor != null && forwardedFor.equals("127.0.0.1")) {
        String origin = httpRequest.getHeader("origin");
        if ((remoteAddr != null && remoteAddr.equals("127.0.0.1") && referer != null && (referer.startsWith("https://bot.benwyw.com/") || referer.startsWith("https://www.benwyw.com/"))) || (origin.equals("https://bot.benwyw.com") || origin.equals("https://www.benwyw.com"))) {
            // Request is coming from localhost, pass through filter chain
            chain.doFilter(request, response);
        } else {
            // Request is not coming from localhost, return error
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(HttpStatus.FORBIDDEN.value());
            httpResponse.getWriter().write("Access denied");
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void destroy() {}
}
