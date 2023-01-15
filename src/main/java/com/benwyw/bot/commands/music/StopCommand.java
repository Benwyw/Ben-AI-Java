package com.benwyw.bot.commands.music;

import com.benwyw.bot.Main;
import com.benwyw.bot.commands.Category;
import com.benwyw.bot.commands.Command;
import com.benwyw.bot.data.GuildData;
import com.benwyw.bot.handler.MusicHandler;
import com.benwyw.util.embeds.EmbedUtils;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

/**
 * Command that clears the music queue and stops music
 *
 * @author Benwyw
 */
public class StopCommand extends Command {

    public StopCommand(Main bot) {
        super(bot);
        this.name = "stop";
        this.description = "Stop the current song and clear the entire music queue.";
        this.category = Category.MUSIC;
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        MusicHandler musicHandler = GuildData.get(event.getGuild()).musicHandler;
        if (musicHandler == null) {
            String text = "The music player is already stopped!";
            event.replyEmbeds(EmbedUtils.createError(text)).setEphemeral(true).queue();
        } else {
            musicHandler.disconnect();
            event.getGuild().getAudioManager().closeAudioConnection();
            String text = ":stop_button: Stopped the music player.";
            event.replyEmbeds(EmbedUtils.createDefault(text)).queue();
        }
    }
}
