package com.benwyw.bot.commands.music;

import com.benwyw.bot.Main;
import com.benwyw.bot.commands.Category;
import com.benwyw.bot.commands.Command;
import com.benwyw.bot.handler.MusicHandler;
import com.benwyw.util.embeds.EmbedUtils;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

/**
 * Command that pauses music player.
 *
 * @author Benwyw
 */
public class PauseCommand extends Command {

    public PauseCommand(Main bot) {
        super(bot);
        this.name = "pause";
        this.description = "Pause the current playing track.";
        this.category = Category.MUSIC;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        MusicHandler music = bot.musicListener.getMusic(event, false);
        if (music == null) return;

        if (music.isPaused()) {
            String text = "The player is already paused!";
            event.replyEmbeds(EmbedUtils.createError(text)).setEphemeral(true).queue();
        } else {
            String text = ":pause_button: Paused the music player.";
            music.pause();
            event.replyEmbeds(EmbedUtils.createDefault(text)).queue();
        }
    }
}
