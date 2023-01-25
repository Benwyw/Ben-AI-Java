package com.benwyw.bot.service;

import java.nio.charset.Charset;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.sharding.ShardManager;

@Slf4j
@Service
public class UserService {
	
	@Autowired
	ShardManager shardManager;
	
	public static HashMap<String, Entry<String, Boolean>> approvalList = new HashMap<String, Entry<String, Boolean>>();
	
	public boolean getApproval(String userTag, String transactionType) {
		User jdaUser = shardManager.getUserByTag(userTag);
		if (!ObjectUtils.isEmpty(jdaUser)) {
		    approvalList.put(userTag, new SimpleEntry<String, Boolean>(transactionType, false));
			jdaUser.openPrivateChannel().flatMap(channel -> channel.sendMessage(String.format("To approve transaction: __%s__\nRespond with `%s`", transactionType, transactionType))).queue();
		}
		
		return false;
	}

	public com.benwyw.bot.payload.User getUserInfo(String userTag) {
		final String transactionType = "getUserInfo";
		com.benwyw.bot.payload.User user = new com.benwyw.bot.payload.User();
		
		if (approvalList.containsKey(userTag) && approvalList.get(userTag).getKey().equals(transactionType)) {
			
			if (approvalList.get(userTag).getKey().equals(transactionType) && approvalList.get(userTag).getValue().equals(true)) {
				User jdaUser = shardManager.getUserByTag(userTag);
				shardManager.getTextChannelById(991895044221046804L).sendMessage(String.format("Requested by: %s", jdaUser)).queue();
				
				
				user.setUserId(jdaUser.getId());
				user.setUserTag(jdaUser.getAsTag());
				user.setUserName(jdaUser.getName());
				
				return user;
			}
			else {
				log.info(String.format("Still pending %s for approval", userTag));
			}
		}
		else {
			getApproval(userTag, transactionType);
		}
		return user;
	}
}
