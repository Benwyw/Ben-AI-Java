package com.benwyw.bot.service;

import com.benwyw.bot.config.DiscordProperties;
import com.benwyw.bot.config.MiscProperties;
import com.benwyw.util.embeds.EmbedColor;
import com.benwyw.util.embeds.EmbedUtils;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class MiscService {
	
	@Autowired
	ShardManager shardManager;

	@Autowired
	EmbedService embedService;

	@Autowired
	private CacheManager cacheManager;

	@Autowired
	private MiscProperties miscProperties;

	@Autowired
	private DiscordProperties discordProperties;

	// TODO miscMapper
//	@Autowired
//	private MiscMapper miscMapper;

	public final static String logChannelName = "FBenI.Logs";

	public String getTitle() {
		return "Ben-AI-Java";
	}

	@Cacheable(value = "userBaseCache", key = "'userBase'")
	public Integer getUserBase() {
//		Integer old = shardManager.getGuilds().stream().mapToInt(guild -> guild.getMemberCount()).sum();
//		Integer old2 = Math.toIntExact(shardManager.getGuilds().stream().flatMap(guild -> guild.loadMembers().get().stream()).distinct().count());
		return Math.toIntExact(shardManager.getGuilds().stream()
				.flatMap(guild -> guild.loadMembers().get().stream())
				.map(member -> member.getId())
				.distinct() // filter out duplicate members
				.count()); // count the remaining distinct members
	}

	@Scheduled(fixedDelay = 3600000) // run every hour
	public void refreshUserBaseCache() {
		Cache cache = cacheManager.getCache("userBaseCache");
		cache.evict("userBase");
		getUserBase(); // call getUserBase to refresh the cache
	}

	/**
	 * Retrieves the user base data from the underlying data source without using the cache.
	 * This method temporarily disables the cache to force a cache miss, retrieves the data from the underlying data source,
	 * and puts the data back into the cache for subsequent calls to retrieve the data from the cache again.
	 * This method should be used sparingly, as it can be expensive to retrieve data from the underlying data source every time.
	 *
	 * @return the Integer object containing the user base data from the underlying data source
	 */
	public Integer getUserBaseWithoutCache() {
		Cache cache = cacheManager.getCache("userBaseCache"); // get the userBaseCache
		cache.clear(); // clear the cache to force a cache miss

		// retrieve the data from the underlying data source
		Integer userBase = getUserBase();

		// put the data back into the cache
		cache.put("userBase", userBase);

		return userBase;
	}

	public MessageEmbed validateJoinedServers() {
		List<Guild> guildList = shardManager.getGuilds();
		long ownerId = shardManager.retrieveApplicationInfo().complete().getOwner().getIdLong();

		List<String> invalidGuildStrList = new ArrayList<>();
		List<String> validGuildStrList = new ArrayList<>();

		List<Guild> invalidGuildList = new ArrayList<>();
		List<Guild> validGuildList = new ArrayList<>();

		MessageEmbed messageEmbed;

		for(Guild guild : guildList) {
			if (ObjectUtils.isEmpty(guild.retrieveMemberById(ownerId))) {
				invalidGuildStrList.add(guild.getName());
				invalidGuildList.add(guild);
			}
			else {
				validGuildStrList.add(guild.getName());
				validGuildList.add(guild);
			}
		}

		messageEmbed = messageToOwner(embedService.validateJoinedServers(validGuildStrList, invalidGuildStrList));

		for(Guild guild : invalidGuildList) {
			try {
				TextChannel defaultChannel = guild.getDefaultChannel().asTextChannel();
				if (defaultChannel != null) {
					defaultChannel.sendMessageEmbeds(EmbedUtils.createError("Leaving non-whitelisted Discord server.")).queue();
				}
			} catch(Exception e){
				messageToLog(String.format("An exception when attempt to notify leaving non-whitelisted Discord server: %s\n%s", guild.getName(), e));
			} finally {
				try {
					guild.leave().queue();
				} catch (Exception e) {
					messageToLog(String.format("An exception when attempt to leave non-whitelisted Discord server: %s\n%s", guild.getName(), e));
				}
			}
		}

		return messageEmbed;
	}

	public boolean validateJoinedServers(Guild guildVerify) {
		List<Guild> guildList = shardManager.getGuilds();
		long ownerId = shardManager.retrieveApplicationInfo().complete().getOwner().getIdLong();

		List<String> invalidGuildStrList = new ArrayList<String>();
		List<String> validGuildStrList = new ArrayList<String>();

		List<Guild> invalidGuildList = new ArrayList<Guild>();
		List<Guild> validGuildList = new ArrayList<Guild>();

		boolean result = false;

		for(Guild guild : guildList) {
			if (ObjectUtils.isEmpty(guild.retrieveMemberById(ownerId))) {
				invalidGuildStrList.add(guild.getName());
				invalidGuildList.add(guild);
			}
			else {
				validGuildStrList.add(guild.getName());
				validGuildList.add(guild);
			}
		}

		messageToOwner(embedService.validateJoinedServers(validGuildStrList, invalidGuildStrList));
		if (validGuildList.contains(guildVerify))
			result = true;

		for(Guild guild : invalidGuildList) {
			try {
				TextChannel defaultChannel = guild.getDefaultChannel().asTextChannel();
				if (defaultChannel != null) {
					defaultChannel.sendMessageEmbeds(EmbedUtils.createError("Leaving non-whitelisted Discord server.")).queue();
				}
			} catch(Exception e){
				messageToLog(String.format("An exception when attempt to notify leaving non-whitelisted Discord server: %s\n%s", guild.getName(), e));
			} finally {
				try {
					guild.leave().queue();
				} catch (Exception e) {
					messageToLog(String.format("An exception when attempt to leave non-whitelisted Discord server: %s\n%s", guild.getName(), e));
				}
			}
		}

		return result;
	}

	public MessageEmbed messageToOwner(MessageEmbed messageEmbed) {
		User owner = shardManager.retrieveApplicationInfo().complete().getOwner();
		owner.openPrivateChannel().flatMap(channel -> channel.sendMessageEmbeds(messageEmbed)).queue();
		return EmbedUtils.createSuccess("Operation completed.");
	}

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

	public MessageEmbed announce(SlashCommandInteractionEvent event) {
		try {
			OptionMapping title = event.getOption("title");
			OptionMapping content = event.getOption("content");
			OptionMapping image = event.getOption("image");

			EmbedBuilder embedBuilder = new EmbedBuilder();
			if (title != null) {
				embedBuilder.setTitle(title.getAsString());
			} else {
				embedBuilder.setTitle("Announcement");
			}
			if (image != null) {
				embedBuilder.setThumbnail(image.getAsAttachment().getUrl());
			}
			embedBuilder.setDescription(content.getAsString());
			embedBuilder.setColor(EmbedColor.DEFAULT.color);
			embedBuilder.setFooter(String.valueOf(LocalDateTime.now(ZoneId.of("Asia/Hong_Kong"))));

			MessageEmbed messaageEmbed = embedBuilder.build();

			for (Long mainChannel : miscProperties.getAnnounce()) {
				TextChannel textChannel = shardManager.getTextChannelById(mainChannel);
				if (textChannel != null) {
					textChannel.sendMessageEmbeds(messaageEmbed).queue();
				} else {
					shardManager.getTextChannelById(discordProperties.getChannels().get("FBenI.Logs")).sendMessageEmbeds(EmbedUtils.createError(String.format("MiscService.announce: send to channelId %s failed", mainChannel))).queue();
				}
			}
		}
		catch(Exception e) {
			return EmbedUtils.createError(String.format("Operation failed.\n%s", e));
		}
		finally {
			return EmbedUtils.createSuccess("Operation completed.");
		}
	}

}
