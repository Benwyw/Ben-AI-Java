package com.benwyw.bot.commands.user;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.CollectionUtils;

import com.benwyw.bot.Main;
import com.benwyw.bot.SpringContext;
import com.benwyw.bot.commands.Category;
import com.benwyw.bot.commands.Command;
import com.benwyw.bot.data.RiotUser;
import com.benwyw.bot.data.User;
import com.benwyw.bot.mapper.RiotUserMapper;
import com.benwyw.bot.mapper.UserMapper;
import com.benwyw.bot.service.RiotService;
import com.benwyw.util.embeds.EmbedColor;
import com.benwyw.util.embeds.EmbedUtils;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

/**
 * Command that configures user details related.
 *
 * @author Benwyw
 */
@Slf4j
public class UserDetailsCommand extends Command {
	
	public static String GAME = "LOL";

    public UserDetailsCommand(Main bot) {
        super(bot);
        this.name = "userdetails";
        this.description = "Query self user details.";
        this.category = Category.UTILITY;
        this.subCommands.add(new SubcommandData("create", "加入RIOT LOL戰績分享。")
        		.addOptions(new OptionData(OptionType.STRING, "riot_region", "RIOT LOL邊區")
        				.addChoice("TW", "TW")
        				.addChoice("NA", "NA")
        				.addChoice("EU", "EU")
        				.setRequired(true),
        				new OptionData(OptionType.STRING, "new_user_name", "名").setRequired(true)));
        this.subCommands.add(new SubcommandData("enquiry", "睇下自己咩料到。"));
        this.subCommands.add(new SubcommandData("update", "更新你的名字。")
        		.addOptions(new OptionData(OptionType.STRING, "riot_region", "RIOT LOL邊區")
        				.addChoice("TW", "TW")
        				.addChoice("NA", "NA")
        				.addChoice("EU", "EU")
        				.setRequired(true),
        				new OptionData(OptionType.STRING, "new_user_name", "新名").setRequired(true)));
        this.subCommands.add(new SubcommandData("delete", "取消RIOT LOL戰績分享。")
        		.addOptions(new OptionData(OptionType.STRING, "riot_region", "RIOT LOL邊區")
        				.addChoice("TW", "TW")
        				.addChoice("NA", "NA")
        				.addChoice("EU", "EU")
        				.setRequired(true)));
//        this.args.add(new OptionData(OptionType.STRING, "message", "Query specific user details"));
//        this.permission = Permission.MANAGE_SERVER;
    }
    
    private String getPointsLolRegion(String region) {
    	String resp = "";
    	
    	switch(region) {
    	case "TW":
    		resp = "loltw";
    		break;
    	case "NA":
    		resp = "lolna";
    		break;
    	case "EU":
    		resp = "loleu";
    		break;
    	}
    	
    	return resp;
    }
    
    private boolean commonValidation(RiotUser riotUser, SlashCommandInteractionEvent event) {
    	RiotUserMapper riotMapper = SpringContext.getBean(RiotUserMapper.class);
    	
    	String text = null;
    	String userId = event.getUser().getId();
    	String newUserName = riotUser.getUserName();
    	String newRegion = riotUser.getRegion();
    	
    	List<RiotUser> existingLinkedUserList = riotMapper.getExistingLinkedUser(riotUser);
		if (existingLinkedUserList.size() > 1) {
			List<RiotUser> otherLinkedUserList = existingLinkedUserList.stream().filter(existingLinkedUser -> !userId.equals(existingLinkedUser.getUserId())).collect(Collectors.toList());
			String otherUserDiscordNames = "";
			for (RiotUser riotUserLinked : otherLinkedUserList) {
				if (StringUtils.isBlank(otherUserDiscordNames))
					otherUserDiscordNames += event.getJDA().getUserById(riotUserLinked.getUserId());
				else
					otherUserDiscordNames += event.getJDA().getUserById(riotUserLinked.getUserId()) + ", ";
			}
			text = String.format("名字 %s 已被 %s 使用。", newUserName, otherUserDiscordNames);
			event.getHook().sendMessageEmbeds(EmbedUtils.createError(text)).queue();
			return false;
		}
		
		RiotService riotService = SpringContext.getBean(RiotService.class);
		if (!riotService.isValidUserName(riotUser)) {
			text = String.format("名字 %s 在 %s 並不存在。", newUserName, newRegion);
			event.getHook().sendMessageEmbeds(EmbedUtils.createError(text)).queue();
			return false;
		}
		
		return true;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        
        RiotUserMapper riotMapper = SpringContext.getBean(RiotUserMapper.class);
        String text = null;
        String userId = event.getUser().getId();
        
        switch(event.getSubcommandName()) {
        case "create" -> {
        	List<RiotUser> riotUserList = riotMapper.getUserInfo(userId);
        	
        	String newRegion = event.getOption("riot_region").getAsString();
            String newUserName = event.getOption("new_user_name").getAsString();
            
        	if (!CollectionUtils.isEmpty(riotUserList)) {
        		for (RiotUser riotUser : riotUserList) {
        			String existingUserName = riotUser.getUserName();
        			String existingRegion = riotUser.getRegion();
        			
	        		if (newRegion.equals(existingRegion)) {
	        			text = String.format("你在 __%s__ 已有現存名字 __%s__，請使用\n`/userdetails update riot_region: %s new_user_name: %s`", existingRegion, existingUserName, existingRegion, newUserName);
	        			event.getHook().sendMessageEmbeds(EmbedUtils.createError(text)).queue();
	        			return;
	        		}
        		}
        	}
        	
    		RiotUser riotUser = new RiotUser();
			riotUser.setUserId(userId);
			riotUser.setUserName(newUserName);
			riotUser.setRegion(newRegion);
			riotUser.setGame(GAME);
			
			if (commonValidation(riotUser, event))
				if (riotMapper.insertUserInfo(riotUser) > 0) {
					String pointsRegion = getPointsLolRegion(newRegion);
					int pointsMapperCount;
					
					if (riotMapper.isExistingPoints(pointsRegion, newUserName))
						pointsMapperCount = riotMapper.insertActivePoints(pointsRegion, newUserName);
					else
						pointsMapperCount = riotMapper.updateToActivePoints(pointsRegion, newUserName);
					
					if (pointsMapperCount > 0)
						text = String.format("已新增 %s: %s", newRegion, newUserName);
					else {
						text = "數據庫出錯。\npointsMapperCount > 0 validation failed. (create)";
						event.getHook().sendMessageEmbeds(EmbedUtils.createError(text)).queue();
	        			return;
					}
				}
				else {
					text = "數據庫出錯。\nriotMapper.insertUserInfo > 0 validation failed. (create)";
					event.getHook().sendMessageEmbeds(EmbedUtils.createError(text)).queue();
        			return;
				}
        }
        case "enquiry" -> {
        	UserMapper userMapper = SpringContext.getBean(UserMapper.class);
            User user = userMapper.getUserInfo(userId);
            
	        if (ObjectUtils.isEmpty(user)) {
	        	text = "Unable to query your user details from our database!";
	        	event.getHook().sendMessageEmbeds(EmbedUtils.createError(text)).queue();
	        	return;
	        }
//        	String dm = EmbedUtils.BLUE_TICK + String.format(" %s %s %s %s %s %s %s %s %s", user.getUserId(), user.getUserBalance(), user.getColorPref(), user.getUserWin(), user.getMcName(), user.getRiotLolTwName(), user.getRiotLolNaName(), user.getOwnedPlaylistCount(), user.getLinkedPlaylistCount());
        	MessageEmbed messageEmbed = new EmbedBuilder()
                    .setTitle("用戶資訊")
                    .setDescription(String.format("ID: `%s`", user.getUserId()))
//                    .setDescription("[" + track.getInfo().title + "](" + track.getInfo().uri + ")")
                    .addField("LoL TW", "`"+user.getRiotLolTwName()+"`", true)
                    .addField("LoL NA", "`"+user.getRiotLolNaName()+"`", true)
                    .addField("LoL EU", "`"+user.getRiotLolEuName()+"`", true)
                    .addField("PL Owned", "`"+user.getOwnedPlaylistCount()+"`", true)
                    .addField("PL Linked", "`"+user.getLinkedPlaylistCount()+"`", true)
                    .addField("MC", "`"+user.getMcName()+"`", true)
//                    .addField("Money", "`"+user.getUserBalance()+"`", true)
//                    .addField("Link", "[`Click Here`]("+track.getInfo().uri+")", true)
                    .setColor(EmbedColor.DEFAULT.color)
                    .setThumbnail(event.getUser().getEffectiveAvatarUrl())
                    .build();
        	
        	event.getUser().openPrivateChannel().flatMap(channel -> channel.sendMessageEmbeds(messageEmbed)).queue();
        	text = "已發送私訊。";
        }
        case "update" -> {
        	List<RiotUser> riotUserList = riotMapper.getUserInfo(userId);
        	
        	String newRegion = event.getOption("riot_region").getAsString();
            String newUserName = event.getOption("new_user_name").getAsString();
            
        	if (CollectionUtils.isEmpty(riotUserList)) {
        		text = String.format("你並沒現存紀錄，請使用\n`/userdetails create riot_region: %s new_user_name: %s`", newRegion, newUserName);
    			event.getHook().sendMessageEmbeds(EmbedUtils.createError(text)).queue();
    			return;
        	}
        	
        	boolean matchedRecord = false;
        	boolean unchangedUserName = false;
        	
        	String existingUserName = "";
			String existingRegion = "";
			
    		for (RiotUser riotUser : riotUserList) {
    			existingUserName = riotUser.getUserName();
    			existingRegion = riotUser.getRegion();
    			
        		if (newRegion.equals(existingRegion)) {
        			matchedRecord = true;
        			if (newUserName.equals(existingUserName))
        				unchangedUserName = true;
        			break;
        		}
    		}
    		
    		if (matchedRecord) {
    			if (unchangedUserName) {
        			text = String.format("名字與現存紀錄一致，並沒改動。");
        			event.getHook().sendMessageEmbeds(EmbedUtils.createError(text)).queue();
        			return;
        		}
    			
	    		RiotUser riotUser = new RiotUser();
				riotUser.setUserId(userId);
				riotUser.setUserName(newUserName);
				riotUser.setRegion(newRegion);
				riotUser.setGame(GAME);
				
				if (commonValidation(riotUser, event))
					if (riotMapper.updateUserInfo(riotUser) > 0) {
						String pointsRegion = getPointsLolRegion(newRegion);
						int pointsMapperCount;
						
						if (riotMapper.isExistingPoints(pointsRegion, newUserName))
							pointsMapperCount = riotMapper.updateExistingActivePoints(pointsRegion, newUserName, existingUserName);
						else
							pointsMapperCount = riotMapper.insertActivePoints(pointsRegion, newUserName);
						
						if (pointsMapperCount > 0)
							text = String.format("%s -> %s", existingUserName, newUserName);
						else {
							text = "數據庫出錯。\npointsMapperCount > 0 validation failed. (update)";
							event.getHook().sendMessageEmbeds(EmbedUtils.createError(text)).queue();
		        			return;
						}
					}
					else {
						text = "數據庫出錯。\nriotMapper.insertUserInfo > 0 validation failed. (update)";
						event.getHook().sendMessageEmbeds(EmbedUtils.createError(text)).queue();
	        			return;
					}
    		}
        }
        case "delete" -> {
        	List<RiotUser> riotUserList = riotMapper.getUserInfo(userId);
        	
        	String newRegion = event.getOption("riot_region").getAsString();
        	boolean matchedRecord = false;
        	String existingUserName = "";
            
        	if (CollectionUtils.isEmpty(riotUserList)) {
        		text = "你並沒任何現存紀錄。";
    			event.getHook().sendMessageEmbeds(EmbedUtils.createError(text)).queue();
    			return;
        	}
        	else {
        		for (RiotUser riotUser : riotUserList) {
        			existingUserName = riotUser.getUserName();
        			String existingRegion = riotUser.getRegion();
        			
	        		if (newRegion.equals(existingRegion)) {
	        			matchedRecord = true;
	        			break;
	        		}
        		}
        	}
        	
        	if (!matchedRecord) {
        		text = String.format("你在 __%s__ 並沒任何現存紀錄。", newRegion);
    			event.getHook().sendMessageEmbeds(EmbedUtils.createError(text)).queue();
    			return;
        	}
        	
    		RiotUser riotUser = new RiotUser();
			riotUser.setUserId(userId);
			riotUser.setRegion(newRegion);
			riotUser.setGame(GAME);
			
			if (riotMapper.deleteUserInfo(riotUser) > 0) {
				String pointsRegion = getPointsLolRegion(newRegion);
				int pointsMapperCount;
				
				if (riotMapper.isExistingPoints(pointsRegion, existingUserName))
					pointsMapperCount = riotMapper.softDeletePoints(pointsRegion, existingUserName);
				else
					pointsMapperCount = riotMapper.insertInactivePoints(pointsRegion, existingUserName);
				
				if (pointsMapperCount > 0)
					text = String.format("已刪除 %s: %s", newRegion, existingUserName);
				else {
					text = "數據庫出錯。\npointsMapperCount > 0 validation failed. (delete)";
					event.getHook().sendMessageEmbeds(EmbedUtils.createError(text)).queue();
        			return;
				}
			}
			else {
				text = "數據庫出錯。";
				event.getHook().sendMessageEmbeds(EmbedUtils.createError(text)).queue();
    			return;
			}
        }
        }
        
        event.getHook().sendMessageEmbeds(EmbedUtils.createSuccess(text)).queue();
    }
}
