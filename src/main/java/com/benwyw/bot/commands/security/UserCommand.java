package com.benwyw.bot.commands.security;

import com.benwyw.bot.Main;
import com.benwyw.bot.SpringContext;
import com.benwyw.bot.commands.Category;
import com.benwyw.bot.commands.Command;
import com.benwyw.bot.service.security.AuthService;
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
public class UserCommand extends Command {

	private final static String ownerId = Dotenv.configure().load().get("OWNER_ID");

    public UserCommand(Main bot) {
        super(bot);
        this.name = "user";
        this.description = "User operations.";
        this.category = Category.UTILITY;
        this.subCommands.add(new SubcommandData("user-insert", "Insert a new application user")
                .addOptions(
                        new OptionData(OptionType.STRING, "username", "Username").setRequired(true).setMaxLength(64),
                        new OptionData(OptionType.STRING, "password", "Password").setRequired(true), // will be hashed
                        new OptionData(OptionType.STRING, "email", "Email address").setRequired(false).setMaxLength(255),
                        new OptionData(OptionType.STRING, "role", "Role (e.g. USER, ADMIN)").setRequired(false).setMaxLength(50),
                        new OptionData(OptionType.STRING, "status", "Account status (e.g. ACTIVE, LOCKED)").setRequired(false).setMaxLength(20),
                        new OptionData(OptionType.STRING, "remarks", "Remarks / notes").setRequired(false).setMaxLength(255)
                )
        );
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

		MessageEmbed messageEmbed = null;
		File file = null;
		String fileName = null;
        
        switch(event.getSubcommandName()) {
            case "user-insert" -> {
                AuthService authService = SpringContext.getBean(AuthService.class);
                messageEmbed = authService.insertUserFromEvent(event);
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
