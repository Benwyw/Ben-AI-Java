package com.benwyw.bot.service;

import com.benwyw.util.embeds.EmbedColor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class EmbedService {
	
//	@Autowired
//	ShardManager shardManager;

	public static MessageEmbed validateJoinedServers(List<String> validGuildList, List<String> invalidGuildList) {
		String validGuildListStr = "";
		String invalidGuildListStr = "";

		for (int i = 0; i < validGuildList.size(); i++) {
			if (i > 0) {
				validGuildListStr += "\n";
			}
			validGuildListStr += validGuildList.get(i);
		}

		for (int i = 0; i < invalidGuildList.size(); i++) {
			if (i > 0) {
				invalidGuildListStr += "\n";
			}
			invalidGuildListStr += invalidGuildList.get(i);
		}

		int color = EmbedColor.ERROR.color;
		if (invalidGuildList.isEmpty())
			color = EmbedColor.SUCCESS.color;

		return new EmbedBuilder()
				.setTitle("Validate Joined Servers")
				.addField("Valid", validGuildListStr, true)
				.addField("Invalid", invalidGuildListStr, true)
				.setColor(color)
//                .setThumbnail(getThumbnail(track))
				.build();
	}
}
