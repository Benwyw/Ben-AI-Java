package com.benwyw.bot;

import javax.security.auth.login.LoginException;

import com.benwyw.bot.listeners.CommandListener;

import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

@Slf4j
public class Main {
	
	private final Dotenv config;
	
	private final ShardManager shardManager;
	
	public Main() throws LoginException {
		config = Dotenv.configure().load();
		String token = config.get("TOKEN");
		
		DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.createDefault(token);
		builder.setStatus(OnlineStatus.ONLINE);
		builder.setActivity(Activity.watching("TV"));
		builder.enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES);
		builder.setMemberCachePolicy(MemberCachePolicy.ALL);
		builder.setChunkingFilter(ChunkingFilter.ALL);
		builder.enableCache(CacheFlag.ONLINE_STATUS);
		shardManager = builder.build();
		
		shardManager.addEventListener(new CommandListener());
	}
	
	public Dotenv getConfig() {
		return config;
	}
	
	public ShardManager getShardManager() {
		return shardManager;
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
