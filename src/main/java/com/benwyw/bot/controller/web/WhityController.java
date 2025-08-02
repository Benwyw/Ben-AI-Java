package com.benwyw.bot.controller.web;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.benwyw.bot.data.WhityWeight;
import com.benwyw.bot.data.WhityWeightReq;
import com.benwyw.bot.service.WhityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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

	@PostMapping("/getWhityWeight")
	public IPage<WhityWeight> getWhityWeight(@RequestBody WhityWeightReq whityWeightReq) {
		return whityService.getWhityWeight(whityWeightReq);
	}

}
