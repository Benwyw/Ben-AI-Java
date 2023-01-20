package com.benwyw.bot;

import javax.security.auth.login.LoginException;

import org.jetbrains.annotations.NotNull;

import com.benwyw.bot.commands.CommandRegistry;
import com.benwyw.bot.data.GuildData;
import com.benwyw.bot.listeners.CommandListener;
import com.benwyw.bot.listeners.MusicListener;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;

@Slf4j
public class Main {
	
	public final @NotNull Dotenv config;
	public final @NotNull ShardManager shardManager;
	public final @NotNull MusicListener musicListener;
	
	public Main() throws LoginException {
		config = Dotenv.configure().load();
		String token = config.get("TOKEN");
		
		DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.createDefault(token);
		builder.setStatus(OnlineStatus.ONLINE);
		builder.setActivity(Activity.watching("音樂幫到你"));
		builder.enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES);
		builder.addEventListeners(new CommandRegistry(this));
		shardManager = builder.build();
		GuildData.init(this);
		
		musicListener = new MusicListener(config.get("SPOTIFY_CLIENT_ID"), config.get("SPOTIFY_TOKEN"));
		shardManager.addEventListener(
				new CommandListener(),
				musicListener);
	}
	
    public static void main( String[] args ) {
    	try {
    		Main bot = new Main();
    		log.info("Bot started.");
    	} catch (LoginException e) {
    		log.error("Provided bot token is invalid!");
    	}
    }
}
