package com.benwyw.bot.controller.web;

import com.benwyw.bot.config.WebSocket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@Slf4j
@RestController
@RequestMapping("ws")
public class MessageController {

	@Autowired
	private WebSocket webSocket;

	// Broadcast to all
	@Profile("local")
	@PostMapping("/sendAllMessage")
	@ResponseStatus(HttpStatus.OK)
	public void sendAllMessage(String message){
		webSocket.sendAllMessage(0, message);
	}

	// Send to target user
	@Profile("local")
	@PostMapping("/sendOneMessage")
	@ResponseStatus(HttpStatus.OK)
	public void sendOneMessage(){
		String message = "[Today topic]: Message to single user";
		webSocket.sendOneMessage(1, message);
	}

	// Send to user group
	@Profile("local")
	@PostMapping("/sendMoreMessage")
	@ResponseStatus(HttpStatus.OK)
	public void sendMoreMessage() {
		String message = "[Today topic]: Message to multi-users";
		webSocket.sendMoreMessage(Arrays.asList(1, 2, 3).toArray(new Integer[0]), message);
	}

	@GetMapping("/generateUserId")
	public Integer generateUserId() {
		return webSocket.generateUserId();
	}

	@GetMapping("/getOnlineUserCount")
	public Integer getOnlineUserCount() {
		return webSocket.getOnlineUserCount();
	}
}