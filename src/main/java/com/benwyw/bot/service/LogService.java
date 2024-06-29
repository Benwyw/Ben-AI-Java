package com.benwyw.bot.service;

import com.benwyw.bot.config.DiscordProperties;
import com.benwyw.util.embeds.EmbedUtils;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service
public class LogService {
	
	@Autowired
	private ShardManager shardManager;

	@Autowired
	private DiscordProperties discordProperties;

	public final static String logChannelName = "FBenI.Logs";

	public void messageToLog(String message) {
		Objects.requireNonNull(shardManager.getTextChannelById(809527650955296848L)).sendMessage(message).queue();
	}

	/**
	 * message = log content
	 * success = EmbedUtils.createDefault / EmbedUtils.createSuccess / EmbedUtils.createError
	 * @param message the content of the message to send
	 * @param success a boolean indicating whether the message is a success message or an error message
	 */
	public void messageToLog(String message, Boolean success) {
		MessageEmbed embed = EmbedUtils.createDefault(message);
		if (success != null) {
			if (success) {
				embed = EmbedUtils.createSuccess(message);
			}
			else {
				embed = EmbedUtils.createError(message);
			}
		}
		Objects.requireNonNull(shardManager.getTextChannelById(discordProperties.getChannels().get(logChannelName))).sendMessageEmbeds(embed).queue();
	}
}
