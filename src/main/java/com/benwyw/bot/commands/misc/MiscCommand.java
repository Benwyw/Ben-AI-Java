package com.benwyw.bot.commands.misc;

import com.benwyw.bot.Main;
import com.benwyw.bot.SpringContext;
import com.benwyw.bot.commands.Category;
import com.benwyw.bot.commands.Command;
import com.benwyw.bot.service.MiscService;
import com.benwyw.util.embeds.EmbedUtils;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.springframework.util.ObjectUtils;

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
        this.subCommands.add(new SubcommandData("validate_joined_servers", "驗證伺服授權。擁有"));
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
//        String userId = event.getUser().getId();
        
        switch(event.getSubcommandName()) {
        	case "validate_joined_servers" -> {
				/**
				 * Owner only command
				 */
				if (event.getJDA().retrieveApplicationInfo().complete().getOwner().getId().equals(event.getUser().getId())) {
					messageEmbed = miscService.validateJoinedServers();
				}
				else {
					messageEmbed = EmbedUtils.createError("You do not have permission to do that.");
				}
        	}
        }

		if (ObjectUtils.isEmpty(messageEmbed)) {
			messageEmbed = EmbedUtils.createError("messageEmbed is empty");
		}

        event.getHook().sendMessageEmbeds(messageEmbed).queue(); //EmbedUtils.createSuccess(text)
    }
}
