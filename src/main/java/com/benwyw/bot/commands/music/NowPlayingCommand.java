package com.benwyw.bot.commands.music;

import com.benwyw.bot.Main;
import com.benwyw.bot.commands.Category;
import com.benwyw.bot.commands.Command;
import com.benwyw.bot.data.GuildData;
import com.benwyw.bot.handler.MusicHandler;
import com.benwyw.util.embeds.EmbedUtils;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

/**
 * Command that displays the currently playing song.
 *
 * @author Benwyw
 */
public class NowPlayingCommand extends Command {

    public NowPlayingCommand(Main bot) {
        super(bot);
        this.name = "playing";
        this.description = "Check what song is currently playing.";
        this.category = Category.MUSIC;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        // Verify the Music Manager isn't null.
        MusicHandler music = GuildData.get(event.getGuild()).musicHandler;
        if (music == null) {
            String text = ":sound: Not currently playing any music!";
            event.replyEmbeds(EmbedUtils.createDefault(text)).setEphemeral(true).queue();
            return;
        }

        // Get currently playing track
        AudioTrack nowPlaying = music.getQueue().size() > 0 ? music.getQueue().getFirst() : null;
        if (nowPlaying == null) {
            String text = ":sound: Not currently playing any music!";
            event.replyEmbeds(EmbedUtils.createDefault(text)).queue();
            return;
        }
        event.replyEmbeds(MusicHandler.displayTrack(nowPlaying, music)).queue();
    }
}
