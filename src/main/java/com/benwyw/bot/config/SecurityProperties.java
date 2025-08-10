package com.benwyw.bot.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

@Component
@Data
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {

    private Set<String> publicPaths = new LinkedHashSet<>();

    // static field to be used like a constant
    private static Set<String> STATIC_PUBLIC_PATHS;

    @PostConstruct
    public void setStaticPublicPaths() {
        // This runs after Spring binds properties into publicPaths
        STATIC_PUBLIC_PATHS = Set.copyOf(publicPaths);
    }

    public static Set<String> getStaticPublicPaths() {
        return STATIC_PUBLIC_PATHS;
    }
}