package com.benwyw.bot.service.web;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MiscService {
	
//	@Autowired
//	ShardManager shardManager;

	// TODO miscMapper
//	@Autowired
//	private MiscMapper miscMapper;

	public String getTitle() {
		return "Ben-AI-Java";
	}

}
