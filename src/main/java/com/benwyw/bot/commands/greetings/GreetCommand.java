package com.benwyw.bot.commands.greetings;

import com.benwyw.bot.Main;
import com.benwyw.bot.commands.Category;
import com.benwyw.bot.commands.Command;
import com.benwyw.util.embeds.EmbedUtils;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

/**
 * Command that configures auto greetings.
 *
 * @author Benwyw
 */
public class GreetCommand extends Command {

    public GreetCommand(Main bot) {
        super(bot);
        this.name = "greet";
        this.description = "Set a greeting to be sent to the welcome channel when a member joins.";
        this.category = Category.GREETINGS;
        this.args.add(new OptionData(OptionType.STRING, "message", "The message to send as a greeting"));
        this.permission = Permission.MANAGE_SERVER;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
//        GreetingHandler greetingHandler = GuildData.get(event.getGuild()).greetingHandler;
        OptionMapping greetingOption = event.getOption("message");

        // Remove greeting message
        if (greetingOption == null) {
//            greetingHandler.removeGreet();
            String text = EmbedUtils.BLUE_X + " Greeting message successfully removed!";
            event.getHook().sendMessageEmbeds(EmbedUtils.createDefault(text)).queue();
            return;
        }

        // Set greeting message
//        greetingHandler.setGreet(greetingOption.getAsString());
        String text = EmbedUtils.BLUE_TICK + " Greeting message successfully updated!";
        event.getHook().sendMessageEmbeds(EmbedUtils.createDefault(text)).queue();
    }
}
