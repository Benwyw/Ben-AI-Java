package com.benwyw.bot.commands.misc;

import com.benwyw.bot.Main;
import com.benwyw.bot.SpringContext;
import com.benwyw.bot.commands.Category;
import com.benwyw.bot.commands.Command;
import com.benwyw.bot.data.MessageEmbedFile;
import com.benwyw.bot.service.MiscService;
import com.benwyw.util.embeds.EmbedUtils;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.ObjectUtils;

import java.io.File;

/**
 * Command that configures user details related.
 *
 * @author Benwyw
 */
@Slf4j
public class MiscCommand extends Command {

    public MiscCommand(Main bot) {
        super(bot);
        this.name = "misc";
        this.description = "Misc operations.";
        this.category = Category.UTILITY;
        this.subCommands.add(new SubcommandData("validate_joined_servers", "Validate server authorization. Owner only command."));
		this.subCommands.add(new SubcommandData("announce", "Broadcast announcement. Owner only command.")
				.addOptions(
						new OptionData(OptionType.STRING, "content", "內容"),
						new OptionData(OptionType.STRING, "title", "標題"),
						new OptionData(OptionType.ATTACHMENT, "image", "圖片"),
						new OptionData(OptionType.BOOLEAN, "minecraft", "Minecraft server version update")
				)
		);
		this.subCommands.add(new SubcommandData("swagger_to_excel", "Convert Swagger text to Excel. (Web only)")
				.addOptions(
						new OptionData(OptionType.ATTACHMENT, "json", "JSON").setRequired(true)
				)
		);
		this.subCommands.add(new SubcommandData("version", "Get bot version."));
		this.subCommands.add(new SubcommandData("unblock", "Remove IP from rate limit list.")
				.addOptions(
						new OptionData(OptionType.STRING, "ipaddress", "IP Address").setRequired(true)
				));
		this.subCommands.add(new SubcommandData("dm", "Send a private message to a user.")
				.addOptions(
						new OptionData(OptionType.USER, "userid", "The user to send the message to.").setRequired(true),
						new OptionData(OptionType.STRING, "message", "The message content to send.").setRequired(true)
				)
		);
		this.subCommands.add(new SubcommandData("delete_messages", "Delete messages contains given keyword.")
				.addOptions(
						new OptionData(OptionType.STRING, "keyword", "The keyword message contains.").setRequired(true),
						new OptionData(OptionType.STRING, "start_date", "The starting date.").setRequired(true)
				)
		);
//        		.addOptions(new OptionData(OptionType.STRING, "riot_region", "RIOT LOL邊區")
//        				.addChoice("TW", "TW")
//        				.addChoice("NA", "NA")
//        				.addChoice("EU", "EU")
//        				.setRequired(true),
//        				new OptionData(OptionType.STRING, "new_user_name", "名").setRequired(true)));
//        this.args.add(new OptionData(OptionType.STRING, "message", "Query specific user details"));
//        this.permission = Permission.MANAGE_SERVER;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

		MiscService miscService = SpringContext.getBean(MiscService.class);
		MessageEmbed messageEmbed = null;
		File file = null;
		String fileName = null;
//        String userId = event.getUser().getId();
        
        switch(event.getSubcommandName()) {
        	case "validate_joined_servers" -> {
				/*
				  Owner only command
				 */
				if (event.getJDA().retrieveApplicationInfo().complete().getOwner().getId().equals(event.getUser().getId())) {
					messageEmbed = miscService.validateJoinedServers();
				}
				else {
					messageEmbed = EmbedUtils.createError("You do not have permission to do that.");
				}
        	}
			case "announce" -> {
				/*
				  Owner only command
				 */
				if (event.getJDA().retrieveApplicationInfo().complete().getOwner().getId().equals(event.getUser().getId())) {
					messageEmbed = miscService.announce(event);
				}
				else {
					messageEmbed = EmbedUtils.createError("You do not have permission to do that.");
				}
			}
			case "swagger_to_excel" -> {
				MessageEmbedFile messageEmbedFile = miscService.swaggerToExcel(event);
				messageEmbed = messageEmbedFile.getMessageEmbed();
				file = messageEmbedFile.getFile();
				fileName = messageEmbedFile.getFileName();
			}
			case "version" -> {
				String version = miscService.getVersion();
				if (StringUtils.isNotBlank(version)) {
					messageEmbed = EmbedUtils.createSuccess(version);
				}
				else {
					messageEmbed = EmbedUtils.createError("Version is empty.");
				}
			}
			case "unblock" -> {
				/*
				  Owner only command
				 */
				if (event.getJDA().retrieveApplicationInfo().complete().getOwner().getId().equals(event.getUser().getId())) {
					messageEmbed = miscService.removeFromRequestsPerIp(event);
				}
				else {
					messageEmbed = EmbedUtils.createError("You do not have permission to do that.");
				}
			}
			case "dm" -> {
				/*
				  Owner only command
				 */
				if (event.getJDA().retrieveApplicationInfo().complete().getOwner().getId().equals(event.getUser().getId())) {
					miscService.sendPrivateMessage(event);
					messageEmbed = EmbedUtils.createDefault("Message sent.");
				}
				else {
					messageEmbed = EmbedUtils.createError("You do not have permission to do that.");
				}
			}
			case "delete_messages" -> {
				/*
				  Owner only command
				 */
				if (event.getJDA().retrieveApplicationInfo().complete().getOwner().getId().equals(event.getUser().getId())) {
					messageEmbed = EmbedUtils.createDefault(miscService.deleteMessagesWithKeyword(event));
				}
				else {
					messageEmbed = EmbedUtils.createError("You do not have permission to do that.");
				}
			}
        }

		if (ObjectUtils.isEmpty(messageEmbed)) {
			messageEmbed = EmbedUtils.createError("messageEmbed is empty");
		}

		if (file != null) {
			if (StringUtils.isNotBlank(fileName)) {
				event.getHook().sendMessageEmbeds(messageEmbed).addFiles(FileUpload.fromData(file, fileName)).queue();
			}
			else {
				event.getHook().sendMessageEmbeds(messageEmbed).addFiles(FileUpload.fromData(file)).queue();
			}
		}
		else {
			event.getHook().sendMessageEmbeds(messageEmbed).queue(); //EmbedUtils.createSuccess(text)
		}
    }
}
