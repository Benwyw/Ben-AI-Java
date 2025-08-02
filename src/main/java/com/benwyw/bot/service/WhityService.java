package com.benwyw.bot.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.benwyw.bot.data.ModeConstant;
import com.benwyw.bot.data.WhityWeight;
import com.benwyw.bot.data.WhityWeightDbReq;
import com.benwyw.bot.data.WhityWeightReq;
import com.benwyw.bot.mapper.WhityMapper;
import com.benwyw.util.FormatUtil;
import com.benwyw.util.embeds.EmbedColor;
import com.benwyw.util.embeds.EmbedUtils;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class WhityService {

	private final static String NULL = "`null`"; // for selectAll display usage

	@Autowired
	private CacheManager cacheManager;

	@Value("${whity.weight.name}")
	private String whityWeightName;

	@Value("${whity.weight.url}")
	private String whityWeightUrl;

	@Autowired
	private LogService logService;

	@Autowired
	private WhityMapper whityMapper;

	@Cacheable(value = "whityWeightCache", key = "'whityWeight'")
	public BigDecimal getLatestWhityWeight() {
		BigDecimal kg = null;
		WhityWeight whityWeight = whityMapper.selectLatest();
		if (!ObjectUtils.isEmpty(whityWeight)) {
			kg = whityWeight.getKg();
		}
		return kg;
	}

	@Scheduled(fixedDelay = 3600000) // run every hour
	public void refreshLatestWhityWeightCache() {
		Cache cache = cacheManager.getCache("whityWeightCache");
		cache.evict("whityWeight");
		getLatestWhityWeight(); // call getLatestWhityWeight to refresh the cache
	}

	public IPage<WhityWeight> getWhityWeight(WhityWeightReq whityWeightReq) {
		Page<WhityWeight> page = new Page<>(whityWeightReq.getPageNumber(), whityWeightReq.getLimit());
		WhityWeightDbReq whityWeightDbReq = new WhityWeightDbReq();
		whityWeightDbReq.setPage(page);
		whityWeightDbReq.setSortBy(whityWeightReq.getSortBy());
		whityWeightDbReq.setSortDesc(whityWeightReq.getSortDesc());
		page.setRecords(whityMapper.getWhityWeight(whityWeightDbReq));
		page.setTotal(whityMapper.getWhityWeightCount());
		return page;
	}

	private StringBuilder append(StringBuilder stringBuilder, String appendText) {
		if (!stringBuilder.isEmpty()) {
			stringBuilder.append("\n");
		}
		stringBuilder.append(appendText);
		return stringBuilder;
	}

	public MessageEmbed weight(SlashCommandInteractionEvent event) {
		MessageEmbed masterMessageEmbed = EmbedUtils.createSuccess("Operation completed.");
		try {
			OptionMapping mode = event.getOption("mode");
			OptionMapping id = event.getOption("id");
			OptionMapping date = event.getOption("date");
			OptionMapping weight = event.getOption("weight");
			OptionMapping remarks = event.getOption("remarks");

			String modeStr = mode != null ? mode.getAsString() : null;
			Integer recordIdInt = id != null ? id.getAsInt() : null;
			String dateStr = date != null ? date.getAsString() : null;
			String weightStr = weight != null ? weight.getAsString() : null;
			String remarksStr = remarks != null ? remarks.getAsString() : null;

			// validation
			if (ModeConstant.INSERT.equals(modeStr) && StringUtils.isBlank(weightStr)) {
				throw new RuntimeException(String.format("`weight` must exists for **%s** operation", modeStr));
			}
			if ((ModeConstant.UPDATE.equals(modeStr) || ModeConstant.DELETE.equals(modeStr)) && recordIdInt == null) {
				throw new RuntimeException(String.format("`id` must exists for **%s** operation", modeStr));
			}

			WhityWeight whityWeight = null;
			if (!ModeConstant.SELECT.equals(modeStr)) {
				if (StringUtils.isBlank(dateStr)) {
					dateStr = FormatUtil.getCurrentLocalDateStr();
				}
				whityWeight = new WhityWeight();
				whityWeight.setRecordId(recordIdInt);
				whityWeight.setRecordDate(FormatUtil.convertStringToLocalDate(dateStr));
				whityWeight.setKg(new BigDecimal(weightStr).setScale(2, RoundingMode.HALF_UP));
				whityWeight.setRemarks(remarksStr);
			}

			int count = 0;
			List<WhityWeight> whityWeightList = new ArrayList<>();
			switch (modeStr) {
				case ModeConstant.SELECT: {
					whityWeightList = whityMapper.select();
					break;
				}
				case ModeConstant.INSERT: {
					count = whityMapper.insert(whityWeight);
					break;
				}
				case ModeConstant.UPDATE: {
					count = whityMapper.update(whityWeight);
					break;
				}
				case ModeConstant.DELETE: {
					count = whityMapper.delete(whityWeight.getRecordId());
					break;
				}
			}

			EmbedBuilder embedBuilder = new EmbedBuilder();
			embedBuilder.setTitle(whityWeightName, whityWeightUrl);
			embedBuilder.setDescription(String.format("%s %s", modeStr, ModeConstant.SELECT.equals(modeStr) || count > 0 ? "successful" : "failure"));
			embedBuilder.setAuthor("Whity");
			embedBuilder.setFooter(String.valueOf(LocalDateTime.now(ZoneId.of("Asia/Hong_Kong"))));
			embedBuilder.setThumbnail("https://i.imgur.com/b81zA3M.png");

			if (ModeConstant.SELECT.equals(modeStr)) {
				StringBuilder recordIdSb = new StringBuilder();
				StringBuilder recordDateSb = new StringBuilder();
				StringBuilder kgSb = new StringBuilder();
				StringBuilder remarksSb = new StringBuilder();

				for (WhityWeight whityWeightItem : whityWeightList) {
					dateStr = String.valueOf(whityWeightItem.getRecordDate());
					weightStr = String.valueOf(whityWeightItem.getKg());
					remarksStr = whityWeightItem.getRemarks();

					append(recordIdSb, whityWeightItem.getRecordId().toString());
					append(recordDateSb, dateStr != null ? dateStr : NULL);
					append(kgSb, weightStr != null ? weightStr : NULL);

					if (!StringUtils.isBlank(remarksStr)) {
						kgSb.append(String.format(" (%s)", remarksStr));
					}
//					append(remarksSb, remarksStr != null? remarksStr : NULL);

				}

				embedBuilder.addField("RECORD_ID", recordIdSb.toString(), true);
				embedBuilder.addField("RECORD_DATE", recordDateSb.toString(), true);
				embedBuilder.addField("KG (REMARKS)", kgSb.toString(), true);
			}
			else {
				embedBuilder.addField("Date", dateStr != null ? dateStr : "", true);
				embedBuilder.addField("KG", weightStr != null ? weightStr : "", true);
				if (StringUtils.isNotBlank(remarksStr)) {
					embedBuilder.addField("Remarks", remarksStr, true);
				}
				logService.messageToLog(String.format("WhityService.weight: %s %s record to database", modeStr, count), true);
			}

			embedBuilder.setColor(ModeConstant.SELECT.equals(modeStr) || count > 0 ? EmbedColor.SUCCESS.color : EmbedColor.ERROR.color);

			masterMessageEmbed = embedBuilder.build();
		}
		catch(Exception e) {
			masterMessageEmbed = EmbedUtils.createError(String.format("Operation failed.\n%s", e));
		}

		return masterMessageEmbed;
	}

}
