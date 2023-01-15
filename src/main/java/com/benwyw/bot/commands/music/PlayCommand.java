package com.benwyw.bot.commands.music;

import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import java.net.MalformedURLException;
import java.net.URL;

import com.benwyw.bot.Main;
import com.benwyw.bot.commands.Category;
import com.benwyw.bot.commands.Command;
import com.benwyw.bot.handler.MusicHandler;
import com.benwyw.util.embeds.EmbedUtils;

/**
 * Command that searches and plays music.
 *
 * @author Benwyw
 */
public class PlayCommand extends Command {

    public PlayCommand(Main bot) {
        super(bot);
        this.name = "play";
        this.description = "Add a song to the queue and play it.";
        this.category = Category.MUSIC;
        this.args.add(new OptionData(OptionType.STRING, "song", "Song to search for or a link to the song", true));
    }

    public void execute(SlashCommandInteractionEvent event) {
        String song = event.getOption("song").getAsString();
        MusicHandler music = bot.musicListener.getMusic(event, true);
        if (music == null) return;

        // Check if member is in the right voice channel
        AudioChannel channel = event.getMember().getVoiceState().getChannel();
        if (music.getPlayChannel() != channel) {
            String text = "You are not in the same voice channel as Ben AI!";
            event.replyEmbeds(EmbedUtils.createError(text)).setEphemeral(true).queue();
            return;
        }

        // Cannot have more than 100 songs in the queue
        if (music.getQueue().size() >= 100) {
            String text = "You cannot queue more than 100 songs!";
            event.replyEmbeds(EmbedUtils.createError(text)).setEphemeral(true).queue();
            return;
        }

        // Find working URL
        String userID = event.getUser().getId();
        try {
            String url;
            try {
                // Check for real URL
                url = new URL(song).toString();
            } catch (MalformedURLException e) {
                // Else search youtube using args
                url = "ytsearch:" + song;
                music.setLogChannel(event.getChannel().asTextChannel());
                bot.musicListener.addTrack(event, url, userID);
                return;
            }
            // Search youtube if using a soundcloud link
            if (url.contains("https://soundcloud.com/")) {
                String[] contents = url.split("/");
                url = "ytsearch:" + contents[3] + "/" + contents[4];
            }
            // Otherwise add real URL to queue
            music.setLogChannel(event.getChannel().asTextChannel());
            bot.musicListener.addTrack(event, url, userID);
        } catch (IndexOutOfBoundsException e) {
            String text = "Please specify a song a to play.";
            event.replyEmbeds(EmbedUtils.createError(text)).setEphemeral(true).queue();
        }
    }
}
