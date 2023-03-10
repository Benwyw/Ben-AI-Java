package com.benwyw.bot.controller.web;

import com.benwyw.bot.data.User;
import com.benwyw.bot.listeners.CommandListener;
import com.benwyw.bot.service.web.MiscService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("misc")
public class MiscController {

//	@Autowired
//	ShardManager shardManager;
	
//	@Autowired
//	CommandListener commandListener;
	
	@Autowired
	MiscService miscService;
	
	@GetMapping("/title")
	public String getTitle(HttpServletRequest request) {
		return miscService.getTitle();
	}

}
