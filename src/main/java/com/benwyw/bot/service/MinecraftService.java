package com.benwyw.bot.service;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.dilley.MineStat;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Getter
@Slf4j
@Service
public class MinecraftService {

	@Value("${minecraft.server.ip}")
	private String minecraftServerIp;

	@Value("${minecraft.server.port}")
	private Integer minecraftServerPort;

	@Value("${minecraft.server.dynmap}")
	private String minecraftServerDynmap;

	@Value("${discord.guild.BenMinecraftServer}")
	private Long discordGuildBenMinecraftServer;

	@Value("${discord.user.DiscordSRV}")
	private Long discordUserDiscordSRV;

	public MineStat getServerStatus() {
		MineStat mineStat = new MineStat(minecraftServerIp, minecraftServerPort);

		// extract version number
		if (ObjectUtils.isNotEmpty(mineStat) && StringUtils.isNotBlank(mineStat.getVersion())) {
			String[] versionArr = mineStat.getVersion().split(" ");
			if (versionArr.length > 1) {
				mineStat.setVersion(versionArr[1]);
			}
		}

		return mineStat;
	}
}
