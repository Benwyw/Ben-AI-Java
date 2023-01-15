package com.benwyw.bot.commands.music;

import com.benwyw.bot.Main;
import com.benwyw.bot.commands.Category;
import com.benwyw.bot.commands.Command;
import com.benwyw.bot.handler.MusicHandler;
import com.benwyw.util.embeds.EmbedUtils;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

/**
 * Command that skips the current song.
 *
 * @author Benwyw
 */
public class SkipCommand extends Command {

    public SkipCommand(Main bot) {
        super(bot);
        this.name = "skip";
        this.description = "Skip the current song.";
        this.category = Category.MUSIC;
    }

    public void execute(SlashCommandInteractionEvent event) {
        MusicHandler music = bot.musicListener.getMusic(event, false);
        if (music == null) return;

        music.skipTrack();
        ReplyCallbackAction action = event.reply(":fast_forward: Skipping...");
        if (music.getQueue().size() == 1) {
            action = action.addEmbeds(EmbedUtils.createDefault(":sound: The music queue is now empty!"));
        }
        action.queue();
    }
}
