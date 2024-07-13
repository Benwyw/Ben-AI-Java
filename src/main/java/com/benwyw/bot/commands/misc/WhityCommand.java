package com.benwyw.bot.commands.misc;

import com.benwyw.bot.Main;
import com.benwyw.bot.SpringContext;
import com.benwyw.bot.commands.Category;
import com.benwyw.bot.commands.Command;
import com.benwyw.bot.data.ModeConstant;
import com.benwyw.bot.service.WhityService;
import com.benwyw.util.embeds.EmbedUtils;
import io.github.cdimascio.dotenv.Dotenv;
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
public class WhityCommand extends Command {

	private final static String ownerId = Dotenv.configure().load().get("OWNER_ID");

    public WhityCommand(Main bot) {
        super(bot);
        this.name = "whity";
        this.description = "Whity operations.";
        this.category = Category.UTILITY;
		this.subCommands.add(new SubcommandData("weight", "Insert Whity weight. Owner only command.")
				.addOptions(
						new OptionData(OptionType.STRING, "mode", "Mode (模式)")
								.addChoice(ModeConstant.SELECT, ModeConstant.SELECT)
								.addChoice(ModeConstant.INSERT, ModeConstant.INSERT)
								.addChoice(ModeConstant.UPDATE, ModeConstant.UPDATE)
								.addChoice(ModeConstant.DELETE, ModeConstant.DELETE).setMaxLength(6).setRequired(true),
						new OptionData(OptionType.NUMBER, "weight", "KG (公斤)").setMaxValue(99.99),
						new OptionData(OptionType.STRING, "date", "yyyyMMdd (日期)").setMaxLength(8),
						new OptionData(OptionType.INTEGER, "id", "Record ID (編號)").setMaxValue(999999999),
						new OptionData(OptionType.STRING, "remarks", "Remarks (備註)").setMaxLength(255)
				)
		);
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

		WhityService whityService = SpringContext.getBean(WhityService.class);
		MessageEmbed messageEmbed = null;
		File file = null;
		String fileName = null;
        
        switch(event.getSubcommandName()) {
			case "weight" -> {
				/*
				  Owner only command
				 */
				if (ownerId.equals(event.getUser().getId())) {
					messageEmbed = whityService.weight(event);
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
