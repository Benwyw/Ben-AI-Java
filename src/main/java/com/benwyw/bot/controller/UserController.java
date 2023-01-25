package com.benwyw.bot.controller;

import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.benwyw.bot.listeners.CommandListener;
import com.benwyw.bot.listeners.MusicListener;
import com.benwyw.bot.payload.User;
import com.benwyw.bot.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.sharding.ShardManager;

@Slf4j
@RestController
@RequestMapping("user")
public class UserController {

	@Autowired
	ShardManager shardManager;
	
	@Autowired
	CommandListener commandListener;
	
	@Autowired
	UserService userService;
	
	@GetMapping("/test")
	public String ok() {
		return String.valueOf(shardManager.getGuildById(763404947500564500L).getRoles());
	}
	
	@GetMapping("/testMsg")
	public void testMsg() {
		shardManager.getTextChannelById(991895044221046804L).sendMessage("This is a test message from Spring Boot web service.").queue();
	}
	
	@GetMapping("/getUserInfo")
	public User getUserInfo(@Param("userTag") String userTag, HttpServletRequest request) {
		log.info(String.format("getUserInfo @ %s", request.getRemoteAddr()));
		return userService.getUserInfo(userTag);
	}
}
