package com.benwyw.bot.service;

import com.benwyw.util.embeds.EmbedUtils;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class MiscService {
	
	@Autowired
	ShardManager shardManager;

	@Autowired
	EmbedService embedService;

	// TODO miscMapper
//	@Autowired
//	private MiscMapper miscMapper;

	public String getTitle() {
		return "Ben-AI-Java";
	}

	public Integer getUserBase() {
//		Integer old = shardManager.getGuilds().stream().mapToInt(guild -> guild.getMemberCount()).sum();
//		Integer old2 = Math.toIntExact(shardManager.getGuilds().stream().flatMap(guild -> guild.loadMembers().get().stream()).distinct().count());
		return Math.toIntExact(shardManager.getGuilds().stream()
				.flatMap(guild -> guild.loadMembers().get().stream())
				.map(member -> member.getId())
				.distinct() // filter out duplicate members
				.count()); // count the remaining distinct members
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
		shardManager.getTextChannelById(809527650955296848L).sendMessage(message).queue();
	}

}
