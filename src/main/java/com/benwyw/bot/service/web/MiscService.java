package com.benwyw.bot.service.web;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MiscService {
	
	@Autowired
	ShardManager shardManager;

	// TODO miscMapper
//	@Autowired
//	private MiscMapper miscMapper;

	public String getTitle() {
		return "Ben-AI-Java";
	}

	public Integer getUserBase() {
		return shardManager.getGuilds().stream().mapToInt(guild -> guild.getMemberCount()).sum();
	}

}
