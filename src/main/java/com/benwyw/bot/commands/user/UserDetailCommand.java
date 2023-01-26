package com.benwyw.bot.commands.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;

import com.benwyw.bot.Main;
import com.benwyw.bot.SpringContext;
import com.benwyw.bot.commands.Category;
import com.benwyw.bot.commands.Command;
import com.benwyw.bot.mapper.UserMapper;
import com.benwyw.bot.payload.User;
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
public class UserDetailCommand extends Command {

    public UserDetailCommand(Main bot) {
        super(bot);
        this.name = "userdetail";
        this.description = "Query self user details.";
        this.category = Category.UTILITY;
//        this.args.add(new OptionData(OptionType.STRING, "message", "Query specific user details"));
//        this.permission = Permission.MANAGE_SERVER;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
//        GreetingHandler greetingHandler = GuildData.get(event.getGuild()).greetingHandler;
        UserMapper userMapper = SpringContext.getBean(UserMapper.class);
        User user = userMapper.getUserInfo(event.getUser().getId());
//        OptionMapping greetingOption = event.getOption("message");

        // Remove greeting message
        if (!ObjectUtils.isEmpty(user)) {
//            greetingHandler.removeGreet();
        	String dm = EmbedUtils.BLUE_TICK + String.format(" %s %s %s %s %s %s %s %s %s", user.getUserId(), user.getUserBalance(), user.getColorPref(), user.getUserWin(), user.getMcName(), user.getRiotLolTwName(), user.getRiotLolNaName(), user.getOwnedPlaylistCount(), user.getLinkedPlaylistCount());
        	event.getUser().openPrivateChannel().flatMap(channel -> channel.sendMessageEmbeds(EmbedUtils.createDefault(dm))).queue();
        	String text = EmbedUtils.BLUE_TICK + " Sent to your DM!";
            event.getHook().sendMessageEmbeds(EmbedUtils.createDefault(text)).queue();
            return;
        }

        // Set greeting message
//        greetingHandler.setGreet(greetingOption.getAsString());
        String text = EmbedUtils.BLUE_X + " Unable to query your user details from our database!";
        event.getHook().sendMessageEmbeds(EmbedUtils.createDefault(text)).queue();
    }
}
