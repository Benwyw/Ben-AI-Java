package com.benwyw.bot.listeners;

import com.benwyw.bot.data.GuildData;
import com.benwyw.bot.handler.MusicHandler;
import com.benwyw.util.SecurityUtils;
import com.benwyw.util.embeds.EmbedUtils;
import com.github.topisenpai.lavasrc.applemusic.AppleMusicSourceManager;
import com.github.topisenpai.lavasrc.spotify.SpotifySourceManager;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import dev.lavalink.youtube.clients.*;
import dev.lavalink.youtube.clients.skeleton.Client;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Module for music player backend and voice channel events.
 *
 * @author Benwyw
 */
@Slf4j
@Component
public class MusicListener extends ListenerAdapter {

    private final @NotNull AudioPlayerManager playerManager;
    
    public MusicListener() {
		this.playerManager = null;
    }

    /**
     * Setup audio player manager.
     */
    public MusicListener(String spotifyClientId, String spotifyClientSecret) {
        this.playerManager = new DefaultAudioPlayerManager();

        // Add Spotify support
        String clientId = spotifyClientId;
        String clientSecret = spotifyClientSecret;
        String countryCode = "HK";
        this.playerManager.registerSourceManager(new SpotifySourceManager(null, clientId, clientSecret, countryCode, playerManager));

        // Add Apple Music support
        String mediaAPIToken = Dotenv.configure().load().get("APPLE_MUSIC_TOKEN");
        playerManager.registerSourceManager(new AppleMusicSourceManager(null, mediaAPIToken, "hk", playerManager));

        // Add YT support
        playerManager.registerSourceManager(new dev.lavalink.youtube.YoutubeAudioSourceManager(/*allowSearch:*/ true, new Client[] { new MusicWithThumbnail(), new WebWithThumbnail(), new AndroidTestsuiteWithThumbnail(), new TvHtml5EmbeddedWithThumbnail(), new AndroidLiteWithThumbnail(), new MediaConnectWithThumbnail(), new IosWithThumbnail() }));

        // Add audio player to source manager
        AudioSourceManagers.registerRemoteSources(playerManager, com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager.class);
    }

    /**
     * Formats track length into a readable string.
     *
     * @param trackLength numerical track length
     * @return string of track length (ex. 2:11)
     */
    public static @NotNull String formatTrackLength(long trackLength) {
        long hours = TimeUnit.MILLISECONDS.toHours(trackLength);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(trackLength) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(trackLength));
        long seconds = TimeUnit.MILLISECONDS.toSeconds(trackLength) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(trackLength));
        String time = "";
        if (hours > 0) time += hours + ":";
        if (minutes < 10 && hours > 0) time += "0";
        time += minutes + ":";
        if (seconds < 10) time += "0";
        time += seconds;
        return time;
    }

    /**
     * Runs a number of validity checks to make sure music player
     * instance is valid before retrieval.
     *
     * @param event The slash command event containing command data.
     * @return Null if invalid status, otherwise music player instance.
     */
    @Nullable
    public MusicHandler getMusic(@NotNull SlashCommandInteractionEvent event, boolean skipQueueCheck) {
        GuildData settings = GuildData.get(event.getGuild());
        // Check if user is in voice channel
        if (!inChannel(Objects.requireNonNull(event.getMember()))) {
            String text = "Please connect to a voice channel first!";
            event.replyEmbeds(EmbedUtils.createError(text)).setEphemeral(true).queue();
            return null;
        }
        // Bot should join voice channel if not already in one.
        AudioChannel channel = Objects.requireNonNull(event.getMember().getVoiceState()).getChannel();
        if (settings.musicHandler == null || !event.getGuild().getAudioManager().isConnected()) {
            assert channel != null;
            joinChannel(settings, channel, event.getChannel().asTextChannel()); // getTextChannel()
        }
        // Check if music is playing in this guild
        if (!skipQueueCheck) {
            if (settings.musicHandler == null || settings.musicHandler.getQueue().isEmpty()) {
                String text = ":sound: There are no songs in the queue!";
                event.replyEmbeds(EmbedUtils.createDefault(text)).queue();
                return null;
            }
            // Check if member is in the right voice channel
            if (settings.musicHandler.getPlayChannel() != channel) {
                String text = "You are not in the same voice channel as Ben AI!";
                event.replyEmbeds(EmbedUtils.createError(text)).setEphemeral(true).queue();
                return null;
            }
        }
        return settings.musicHandler;
    }

    /**
     * Joins a voice channel.
     *
     * @para guildData    The GuilData instance for this guild.
     * @param channel    The Voice Channel.
     * @param logChannel A log channel to notify users.
     */
    public void joinChannel(@NotNull GuildData guildData, @NotNull AudioChannel channel, TextChannel logChannel) {
        AudioManager manager = channel.getGuild().getAudioManager();
        if (guildData.musicHandler == null) {
            guildData.musicHandler = new MusicHandler(playerManager.createPlayer());
        }
        manager.setSendingHandler(guildData.musicHandler);
        Objects.requireNonNull(guildData.musicHandler).setLogChannel(logChannel);
        guildData.musicHandler.setPlayChannel(channel);
        manager.openAudioConnection(channel);
    }

    /**
     * Checks whether the specified member is in a voice channel.
     *
     * @param member The specified Member
     * @return True if this member is in a voice channel, otherwise false.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean inChannel(@NotNull Member member) {
        return member.getVoiceState() != null && member.getVoiceState().inAudioChannel();
    }

    /**
     * Add a track to the specified guild.
     *
     * @param event  A slash command event.
     * @param url    The track URL.
     * @param userID   The ID of the user that added this track.
     */
    public void addTrack(SlashCommandInteractionEvent event, String url, String userID) {
        MusicHandler music = GuildData.get(event.getGuild()).musicHandler;
        if (music == null) return;

        // Check for SSRF vulnerability with whitelist
        try {
            boolean isWhitelisted = SecurityUtils.isUrlWhitelisted(url);
            if(!isWhitelisted) {
                url = "";
            }
        } catch(MalformedURLException ignored) {}
        playerManager.loadItem(url, new AudioLoadResultHandler() {

            @Override
            public void trackLoaded(@NotNull AudioTrack audioTrack) {
                audioTrack.setUserData(userID);
                music.enqueue(audioTrack);
                event.reply(":notes: | Added **"+audioTrack.getInfo().title+"** to the queue.").queue();
            }

            @Override
            public void playlistLoaded(@NotNull AudioPlaylist audioPlaylist) {
                // Queue first result if youtube search
                if (audioPlaylist.isSearchResult()) {
                    trackLoaded(audioPlaylist.getTracks().get(0));
                    return;
                }

                // Otherwise load first 100 tracks from playlist
                int total = audioPlaylist.getTracks().size();
                if (total > 100) total = 100;
                event.reply(":notes: | Added **"+audioPlaylist.getName()+"** with `"+total+"` songs to the queue.").queue();

                total = music.getQueue().size();
                for (AudioTrack track : audioPlaylist.getTracks()) {
                    if (total < 100) {
                        music.enqueue(track);
                    }
                    total++;
                }
            }

            @Override
            public void noMatches() {
                String msg = "That is not a valid song!";
                event.replyEmbeds(EmbedUtils.createError(msg)).setEphemeral(true).queue();
            }

            @Override
            public void loadFailed(FriendlyException e) {
                String msg = "That is not a valid link!";
                event.replyEmbeds(EmbedUtils.createError(msg)).setEphemeral(true).queue();
            }
        });
    }

    
    
    @Override
	public void onGuildVoiceUpdate(GuildVoiceUpdateEvent event) {
    	if (event.getChannelLeft() != null && event.getChannelLeft().getMembers().size() == 1 && event.getJDA().getSelfUser().getIdLong() == event.getChannelLeft().getMembers().get(0).getIdLong()) {
    		GuildData data = GuildData.get(event.getGuild());
            if (data.musicHandler != null) {
                data.musicHandler.disconnect();
                event.getGuild().getAudioManager().closeAudioConnection();
            }
            log.info(String.format("Only bot left in voice channel: %s, leaving...", String.valueOf(event.getChannelLeft())));
    	}
    	
    	if (event.getChannelJoined() != null) { // move
	    	if (event.getJDA().getSelfUser().getIdLong() == event.getMember().getIdLong()) {
	            GuildData data = GuildData.get(event.getGuild());
	            if (data.musicHandler != null) {
	                data.musicHandler.setPlayChannel(event.getChannelJoined());
	            }
	        }
    	}
    	else { // leave
    		if (event.getJDA().getSelfUser().getIdLong() == event.getMember().getIdLong()) {
                GuildData data = GuildData.get(event.getGuild());
                if (data.musicHandler != null) {
                    data.musicHandler.disconnect();
                }
            }
    	}
	}
}
