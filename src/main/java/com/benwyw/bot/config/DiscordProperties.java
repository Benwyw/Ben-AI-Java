package com.benwyw.bot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
//@PropertySource("classpath:settings.properties")
@Data
@ConfigurationProperties(prefix = "discord")
public class DiscordProperties {

	/**
	 * discord.guilds.GUILD_NAME=GUILD_ID
	 */
//	private Map<String, Long> guilds;

	/**
	 * discord.channels.GUILD_NAME.CHANNEL_NAME=CHANNEL_ID
	 */
	private Map<String, Long> channels; // guildName.channelName

}