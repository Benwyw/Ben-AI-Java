package com.benwyw.bot.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.benwyw.bot.listeners.CommandListener;
import com.benwyw.bot.listeners.MusicListener;

import net.dv8tion.jda.api.sharding.ShardManager;

@RestController
@RequestMapping("user")
public class UserController {

	@Autowired
	ShardManager shardManager;
	
	@Autowired
	CommandListener commandListener;
	
	@GetMapping("/test")
	public String ok() {
		return String.valueOf(shardManager.getGuildById(763404947500564500L).getRoles());
	}
	
	@GetMapping("/testMsg")
	public void testMsg() {
		shardManager.getTextChannelById(991895044221046804L).sendMessage("This is a test message from Spring Boot web service.").queue();
	}
}
