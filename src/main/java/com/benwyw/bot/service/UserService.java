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

import com.benwyw.bot.mapper.UserMapper;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.sharding.ShardManager;

@Slf4j
@Service
public class UserService {
	
	@Autowired
	ShardManager shardManager;
	
	@Autowired
	private UserMapper userMapper;
	
	public static HashMap<String, Entry<String, Boolean>> approvalList = new HashMap<String, Entry<String, Boolean>>();
	public static HashMap<String, String> transactionMap = new HashMap<String, String>();
	
	// TODO enable unique multiple transactions per user
	private String generateTransactionId(String transactionType) {
		String generatedString = null;
		
		do {
			byte[] array = new byte[7]; // length is bounded by 7
		    new Random().nextBytes(array);
		    generatedString = new String(array, Charset.forName("UTF-8"));
		} while (transactionMap.containsKey(generatedString));
	    
	    transactionMap.put(generatedString, transactionType);
	    return generatedString;
	}
	
	public boolean getApproval(String userTag, String transactionType) {
		User jdaUser = shardManager.getUserByTag(userTag);
		if (!ObjectUtils.isEmpty(jdaUser)) {
			if (approvalList.containsKey(userTag) && approvalList.get(userTag).getKey().equals(transactionType)) {
				return approvalList.get(userTag).getValue();
			}
			else {
				approvalList.put(userTag, new SimpleEntry<String, Boolean>(transactionType, false));
				jdaUser.openPrivateChannel().flatMap(channel -> channel.sendMessage(String.format("To approve transaction: __%s__\nRespond with `%s`", transactionType, transactionType))).queue();
			}
		}
		
		return false;
	}

	public com.benwyw.bot.payload.User getUserInfo(String userTag) {
		final String transactionType = "getUserInfo";
		com.benwyw.bot.payload.User user = new com.benwyw.bot.payload.User();
		
		if (getApproval(userTag, transactionType)) {
			approvalList.remove(userTag);
			
			User jdaUser = shardManager.getUserByTag(userTag);
			shardManager.getTextChannelById(809527650955296848L).sendMessage(String.format("Requested by: %s", jdaUser.getName())).queue();
			
			user = userMapper.getUserInfo(jdaUser.getId());
			
			user.setUserId(jdaUser.getId());
			user.setUserTag(jdaUser.getAsTag());
			user.setUserName(jdaUser.getName());
			
			return user;
		}
		else {
			log.info(String.format("Still pending %s for approval", userTag));
		}
		
		return user;
	}
}
