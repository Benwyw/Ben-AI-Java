package com.benwyw.bot.commands.music;

import com.benwyw.bot.Main;
import com.benwyw.bot.commands.Category;
import com.benwyw.bot.commands.Command;
import com.benwyw.bot.handler.MusicHandler;
import com.benwyw.util.embeds.EmbedUtils;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

/**
 * Command that toggles repeat mode for music queue.
 *
 * @author Benwyw
 */
public class RepeatCommand extends Command {

    public RepeatCommand(Main bot) {
        super(bot);
        this.name = "repeat";
        this.description = "Toggles the repeat mode.";
        this.category = Category.MUSIC;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        MusicHandler music = bot.musicListener.getMusic(event, false);
        if (music == null) return;

        music.loop();
        String text;
        if (music.isLoop()) {
            text = ":repeat: Repeat has been enabled.";
        } else {
            text = ":repeat: Repeat has been disabled.";
        }
        event.replyEmbeds(EmbedUtils.createDefault(text)).queue();
    }
}
