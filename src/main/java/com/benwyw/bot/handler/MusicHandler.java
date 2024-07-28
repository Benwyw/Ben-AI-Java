package com.benwyw.bot.handler;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.audio.AudioSendHandler;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.benwyw.bot.listeners.MusicListener;
import com.benwyw.util.SecurityUtils;
import com.benwyw.util.embeds.EmbedColor;
import com.benwyw.util.embeds.EmbedUtils;
import com.github.topisenpai.lavasrc.mirror.MirroringAudioTrack;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;

import java.nio.ByteBuffer;
import java.util.LinkedList;

/**
 * Handles music for each guild with a unique queue and audio player for each.
 *
 * @author Benwyw
 */
public class MusicHandler implements AudioSendHandler {

    /** LavaPlayer essentials. */
    public final @NotNull AudioPlayer audioPlayer;
    private AudioFrame lastFrame;

    /** Queue of music tacks in FIFO order. */
    private final @NotNull LinkedList<AudioTrack> queue;

    /** The text channel in which the bot is logging music actions. */
    private TextChannel logChannel;

    /** The voice channel in which the bot is playing music. */
    private @Nullable AudioChannel playChannel;

    /** Whether the music player is on loop. */
    private boolean isLoop;
    private boolean isSkip;

    public MusicHandler(@NotNull AudioPlayer audioPlayer) {
        this.audioPlayer = audioPlayer;
        this.queue = new LinkedList<>();
        this.isLoop = false;
        this.isSkip = false;
        TrackScheduler scheduler = new TrackScheduler(this);
        audioPlayer.addListener(scheduler);
    }

    /**
     * Queue a new song to the audio player.
     *
     * @param track audio track to be queued.
     */
    public void enqueue(AudioTrack track) {
        queue.addLast(track);
        if (audioPlayer.getPlayingTrack() == null) {
            audioPlayer.playTrack(queue.getFirst());
        }
    }

    /**
     * Pause audio player.
     */
    public void pause() {
        audioPlayer.setPaused(true);
    }

    /**
     * Resume audio player.
     */
    public void unpause() {
        audioPlayer.setPaused(false);
    }

    /**
     * Check is audio player is paused.
     */
    public boolean isPaused() {
        return audioPlayer.isPaused();
    }

    /**
     * Disconnects from the voice channel and clears queue.
     */
    public void disconnect() {
        playChannel = null;
        queue.clear();
        audioPlayer.stopTrack();
    }

    /**
     * Sets the volume level of the audio player.
     *
     * @param volume volume level between 0-100
     */
    public void setVolume(int volume) {
        audioPlayer.setVolume(volume);
    }

    /**
     * Skips the current playing track.
     */
    public void skipTrack() {
        isSkip = true;
        audioPlayer.getPlayingTrack().setPosition(audioPlayer.getPlayingTrack().getDuration());
    }

    /**
     * Skips to a specified track in the queue.
     *
     * @param pos Position in the queue to skip to.
     */
    public void skipTo(int pos) {
        if (pos > 1) {
            queue.subList(1, pos).clear();
        }
        skipTrack();
    }

    /**
     * Sets the position of the current track.
     *
     * @param position position in current track in milliseconds.
     */
    public void seek(long position) {
        audioPlayer.getPlayingTrack().setPosition(position);
    }

    /**
     * Get the audio player queue.
     *
     * @return list of tracks in queue. Returns copy to avoid external modification.
     */
    public @NotNull LinkedList<AudioTrack> getQueue() {
        return new LinkedList<>(queue);
    }

    /**
     * Get the voice channel the bot is playing music in.
     *
     * @return voice channel playing music.
     */
    public @Nullable AudioChannel getPlayChannel() {
        return playChannel;
    }

    /**
     * Sets the music play channel.
     *
     * @param channel voice channel to set as play channel.
     */
    public void setPlayChannel(@Nullable AudioChannel channel) {
        playChannel = channel;
    }

    /**
     * Get the text channel that logs music related info and commands.
     *
     * @return text channel logging music info.
     */
    public TextChannel getLogChannel() {
        return logChannel;
    }

    /**
     * Sets the music log channel.
     *
     * @param channel text channel to set as log channel.
     */
    public void setLogChannel(TextChannel channel) {
        logChannel = channel;
    }

    /**
     * Determines whether track is looping.
     *
     * @return true or false based on isLoop
     */
    public boolean isLoop() {
        return isLoop;
    }

    /**
     * Flips loop status like a switch.
     */
    public void loop() {
        isLoop = !isLoop;
    }

    @Override
    public boolean canProvide() {
        lastFrame = audioPlayer.provide();
        return lastFrame != null;
    }

    @Override
    public boolean isOpus() {
        return true;
    }

    @Nullable
    @Override
    public ByteBuffer provide20MsAudio() {
        return ByteBuffer.wrap(lastFrame.getData());
    }

    /**
     * Manages audio events and schedules tracks.
     */
    public static class TrackScheduler extends AudioEventAdapter {

        private final MusicHandler handler;

        public TrackScheduler(MusicHandler handler) {
            this.handler = handler;
        }

        /**
         * Creates an embed message when a track starts that displays relevant info.
         *
         * @param player the audio player
         * @param track  the track that is starting.
         */
        @Override
        public void onTrackStart(AudioPlayer player, @NotNull AudioTrack track) {
            handler.logChannel.sendMessageEmbeds(displayTrack(track, handler)).queue();
        }

        @Override
        public void onTrackEnd(@NotNull AudioPlayer player, @NotNull AudioTrack track, @NotNull AudioTrackEndReason endReason) {
            if (handler.isLoop() && !handler.isSkip) {
                // Loop current track
                handler.queue.set(0, track.makeClone());
                player.playTrack(handler.queue.getFirst());
            } else if (!handler.queue.isEmpty()) {
                // Play next track in queue
                handler.isSkip = false;
                handler.queue.removeFirst();
                if (endReason.mayStartNext && handler.queue.size() > 0) {
                    player.playTrack(handler.queue.getFirst());
                }
            }
        }

        @Override
        public void onTrackException(AudioPlayer player, AudioTrack track, @NotNull FriendlyException exception) {
            String msg = String.format("You're advised to use <@!1267103861542490162> due to an error occurred! %s", exception.getMessage());
            handler.logChannel.sendMessageEmbeds(EmbedUtils.createError(msg)).queue();
            exception.printStackTrace();
        }

        @Override
        public void onTrackStuck(@NotNull AudioPlayer player, AudioTrack track, long thresholdMs) {
            String msg = "Track got stuck, attempting to fix...";
            handler.logChannel.sendMessageEmbeds(EmbedUtils.createError(msg)).queue();
            handler.queue.remove(track);
            player.stopTrack();
            player.playTrack(handler.queue.getFirst());
        }
    }

    /**
     * Creates a thumbnail URL with the track image.
     * @param track the AudioTrack object from the music player.
     *
     * @return a URL to the song video thumbnail.
     */
    private static String getThumbnail(AudioTrack track) {
        String domain = SecurityUtils.getDomain(track.getInfo().uri);
        if (domain.equalsIgnoreCase("spotify") || domain.equalsIgnoreCase("apple")) {
            return ((MirroringAudioTrack) track).getArtworkURL();
        }
        return String.format("https://img.youtube.com/vi/%s/0.jpg", track.getIdentifier());
    }

    /**
     * Creates an embed displaying details about a track.
     *
     * @param track the track to display details about.
     * @param handler the music handler instance.
     * @return a MessageEmbed displaying track details.
     */
    public static MessageEmbed displayTrack(AudioTrack track, MusicHandler handler) {
        String duration = MusicListener.formatTrackLength(track.getInfo().length);
        String repeat = (handler.isLoop()) ? "Enabled" : "Disabled";
        String userMention = "<@!"+track.getUserData(String.class)+">";
        return new EmbedBuilder()
                .setTitle("Now Playing")
                .setDescription("[" + track.getInfo().title + "](" + track.getInfo().uri + ")")
                .addField("Duration", "`"+duration+"`", true)
                .addField("Queue", "`"+(handler.queue.size()-1)+"`", true)
                .addField("Volume", "`"+handler.audioPlayer.getVolume()+"%`", true)
                .addField("Requester", userMention, true)
                .addField("Link", "[`Click Here`]("+track.getInfo().uri+")", true)
                .addField("Repeat", "`"+repeat+"`", true)
                .setColor(EmbedColor.DEFAULT.color)
                .setThumbnail(getThumbnail(track))
                .build();
    }
}
