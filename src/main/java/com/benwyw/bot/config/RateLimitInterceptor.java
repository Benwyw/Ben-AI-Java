package com.benwyw.bot.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final int RATE_LIMIT = 10;
    private static final int RATE_LIMIT_PERIOD_IN_SECONDS = 60;
    private static final Map<String, Integer> requestsPerIp = new ConcurrentHashMap<>();

    @Autowired
    ShardManager shardManager;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ipAddress = request.getHeader("X-Forwarded-For"); //request.getRemoteAddr();
        int requests = requestsPerIp.getOrDefault(ipAddress, 0);
        if (requests >= RATE_LIMIT) {
            try {
                shardManager.getTextChannelById(809527650955296848L).sendMessage(String.format("Rate limited: %s", ipAddress)).queue();
            } catch(Exception e) {
                log.info(String.format("An exception occurred during logging rate limit ipAddress to Discord:\n%s",e));
            }
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Too many requests");
            return false;
        } else {
            requestsPerIp.put(ipAddress, requests + 1);
            return true;
        }
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // No-op
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // Reset request count for IP address after rate limit period has elapsed
        String ipAddress = request.getRemoteAddr();
        requestsPerIp.computeIfPresent(ipAddress, (key, value) -> {
            if (System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(RATE_LIMIT_PERIOD_IN_SECONDS) > value) {
                return null;
            }
            return value;
        });
    }
}
