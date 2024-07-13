package com.benwyw.bot.controller.web;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.benwyw.bot.data.WhityWeight;
import com.benwyw.bot.service.WhityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@Slf4j
@RestController
@RequestMapping("whity")
public class WhityController {

	@Autowired
	private WhityService whityService;

	@GetMapping("/getLatestWhityWeight")
	public BigDecimal getLatestWhityWeight() {
		return whityService.getLatestWhityWeight();
	}

	@GetMapping("/getWhityWeight")
	public IPage<WhityWeight> getWhityWeight(@RequestParam("pageNumber") int pageNumber, @RequestParam("limit") int limit) {
		return whityService.getWhityWeight(pageNumber, limit);
	}

}
