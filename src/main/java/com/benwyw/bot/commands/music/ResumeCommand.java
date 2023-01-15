package com.benwyw.bot.commands.music;

import com.benwyw.bot.Main;
import com.benwyw.bot.commands.Category;
import com.benwyw.bot.commands.Command;
import com.benwyw.bot.handler.MusicHandler;
import com.benwyw.util.embeds.EmbedUtils;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

/**
 * Command that un-pauses music player.
 *
 * @author Benwyw
 */
public class ResumeCommand extends Command {

    public ResumeCommand(Main bot) {
        super(bot);
        this.name = "resume";
        this.description = "Resumes the current paused track.";
        this.category = Category.MUSIC;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        MusicHandler music = bot.musicListener.getMusic(event, false);
        if (music == null) return;

        if (music.isPaused()) {
            music.unpause();
            String text = ":play_pause: Resuming the music player.";
            event.replyEmbeds(EmbedUtils.createDefault(text)).queue();
        } else {
            String text = "The player is not paused!";
            event.replyEmbeds(EmbedUtils.createError(text)).setEphemeral(true).queue();
        }
    }
}
