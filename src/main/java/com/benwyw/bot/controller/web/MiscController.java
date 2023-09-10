package com.benwyw.bot.controller.web;

import com.benwyw.bot.service.MiscService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
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

	@GetMapping("/userBase")
	public Integer getUserBase() {
		return miscService.getUserBase();
	}

	@GetMapping("/version")
	public String getVersion() {
		return miscService.getVersion();
	}

	@GetMapping("/getFeatures")
	public IPage<Feature> getFeatures(@RequestParam("pageNumber") int pageNumber, @RequestParam("limit") int limit) {
		return miscService.getFeatures(pageNumber, limit);
	}

}
