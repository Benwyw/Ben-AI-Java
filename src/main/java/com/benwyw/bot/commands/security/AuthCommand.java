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
import net.dv8tion.jda.api.interactions.components.buttons.Button;
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
public class AuthCommand extends Command {

	private final static String ownerId = Dotenv.configure().load().get("OWNER_ID");
	public static final int PAGE_SIZE = 10;

    public AuthCommand(Main bot) {
        super(bot);
        this.name = "auth";
        this.description = "Auth operations.";
        this.category = Category.UTILITY;
        this.subCommands.add(new SubcommandData("user-insert", "Insert a new application user")
                .addOptions(
                        new OptionData(OptionType.STRING, "username", "Username").setRequired(true).setMaxLength(64),
                        new OptionData(OptionType.STRING, "password", "Password").setRequired(true), // will be hashed
                        new OptionData(OptionType.STRING, "email", "Email address").setRequired(false).setMaxLength(255),
                        new OptionData(OptionType.STRING, "role", "Role (e.g. USER, ADMIN)").setRequired(false).setMaxLength(50)
                                .addChoice("USER", "USER")
                                .addChoice("ADMIN", "ADMIN"),
                        new OptionData(OptionType.STRING, "status", "Account status (e.g. ACTIVE, LOCKED)").setRequired(false).setMaxLength(20)
                                .addChoice("ACTIVE", "ACTIVE")
                                .addChoice("LOCKED", "LOCKED"),
                        new OptionData(OptionType.STRING, "remarks", "Remarks / notes").setRequired(false).setMaxLength(255)
                )
        );
        this.subCommands.add(
        new SubcommandData("user-delete", "Delete a user by username")
                .addOptions(new OptionData(OptionType.STRING, "username", "The username of the user to delete").setRequired(true))
        );
        this.subCommands.add(
        new SubcommandData("token-purge", "Delete expired or revoked refresh tokens")
                .addOptions(new OptionData(OptionType.BOOLEAN, "dryrun", "Only count; don't delete"))
        );
        this.subCommands.add(
        new SubcommandData("user-list", "List all users with pagination")
                .addOptions(new OptionData(OptionType.INTEGER, "page", "Page number (default: 1)").setRequired(false).setMinValue(1))
        );
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        AuthService authService = SpringContext.getBean(AuthService.class);
		MessageEmbed messageEmbed = null;
		File file = null;
		String fileName = null;
        
        switch(event.getSubcommandName()) {
            case "user-insert" -> {
                messageEmbed = authService.insertUserFromEvent(event);
            }
            case "token-purge" -> {
                messageEmbed = authService.purgeRefreshTokensFromEvent(event);
            }
            case "user-delete" -> {
                messageEmbed = authService.deleteUserFromEvent(event);
            }
            case "user-list" -> {
                int page = event.getOption("page") != null ? event.getOption("page").getAsInt() : 1;
                AuthService.UserListResult result = authService.listUsersFromEvent(page, PAGE_SIZE);
                messageEmbed = result.getEmbed();

                // Add pagination buttons if there are multiple pages
                if (result.getTotalPages() > 1) {
                    Button prevBtn = Button.primary("auth_userlist_prev_" + result.getCurrentPage(), "◀ Previous")
                            .withDisabled(!result.hasPrevious());
                    Button nextBtn = Button.primary("auth_userlist_next_" + result.getCurrentPage(), "Next ▶")
                            .withDisabled(!result.hasNext());
                    event.getHook().sendMessageEmbeds(messageEmbed).addActionRow(prevBtn, nextBtn).queue();
                    return;
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
