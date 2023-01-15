package com.benwyw.bot.commands.music;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import org.jetbrains.annotations.NotNull;

import com.benwyw.bot.Main;
import com.benwyw.bot.commands.Category;
import com.benwyw.bot.commands.Command;
import com.benwyw.bot.handler.MusicHandler;
import com.benwyw.util.embeds.EmbedUtils;

/**
 * Command that changes volume of the music player.
 *
 * @author Benwyw
 */
public class VolumeCommand extends Command {

    public VolumeCommand(Main bot) {
        super(bot);
        this.name = "volume";
        this.description = "Changes the volume of the music.";
        this.category = Category.MUSIC;
        this.args.add(new OptionData(OptionType.INTEGER, "amount", "Enter value between 0-100 to set", true)
                .setMinValue(0)
                .setMaxValue(100));
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        int volume = event.getOption("amount").getAsInt();
        MusicHandler music = bot.musicListener.getMusic(event, true);
        if (music == null) return;
        try {
            if (volume < 0 || volume > 100) {
                throw new NumberFormatException();
            }
            music.setVolume(volume);
            String text = String.format(":loud_sound: Set the volume to `%s%%`", volume);
            event.replyEmbeds(EmbedUtils.createDefault(text)).queue();
            return;
        } catch (@NotNull NumberFormatException | ArrayIndexOutOfBoundsException ignored) {}

        String text = "You must specify a volume between 0-100";
        event.replyEmbeds(EmbedUtils.createError(text)).queue();
    }
}
