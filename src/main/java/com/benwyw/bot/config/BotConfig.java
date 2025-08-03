package com.benwyw.bot.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BotConfig {

    private final Dotenv dotenv = Dotenv.configure().load();

    @Bean
    @Qualifier("spotifyClientId")
    public String spotifyClientId() {
        return dotenv.get("SPOTIFY_CLIENT_ID");
    }

    @Bean
    @Qualifier("spotifyClientSecret")
    public String spotifyClientSecret() {
        return dotenv.get("SPOTIFY_TOKEN");
    }
}

