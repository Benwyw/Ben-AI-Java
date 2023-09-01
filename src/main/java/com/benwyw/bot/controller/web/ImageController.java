package com.benwyw.bot.controller.web;

import com.benwyw.bot.service.ImageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("image")
public class ImageController {

//	@Autowired
//	ShardManager shardManager;
	
//	@Autowired
//	CommandListener commandListener;
	
	@Autowired
	private ImageService imageService;

	/**
	 * Calculate aHash with image String passed in Frontend
	 * @param base64Image String
	 * @return String
	 */
	@PostMapping("/aHash")
	public ResponseEntity<String> calculateAHash(@RequestBody String base64Image) {
		return imageService.calculateAHash(base64Image);
	}

}
