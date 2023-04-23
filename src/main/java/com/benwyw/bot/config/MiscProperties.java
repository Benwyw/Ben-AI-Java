package com.benwyw.bot.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Data
@ConfigurationProperties(prefix = "misc")
public class MiscProperties {

	/**
	 * misc.LIST_NAME[INT]=${CHANNEL.PROPERTIES}
	 */
	private List<Long> announce;

}