package com.benwyw.bot.listeners;

import java.util.ArrayList;
import java.util.List;

import com.benwyw.bot.commands.RolesCommand;

import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class CommandListener extends ListenerAdapter {

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		String command = event.getName();
		
		switch(command) {
		case "welcome":
			String userTag = event.getUser().getAsTag();
			event.reply(String.format("WELCOME **%s**!",userTag)).queue();
			break;
		case "roles":
			new RolesCommand(event);
			break;
		}
	}

	/**
	 * Guild command -- instantly updated (max 100)
	 */
	@Override
	public void onGuildReady(GuildReadyEvent event) {
		List<CommandData> commandData = new ArrayList<>();
		commandData.add(Commands.slash("welcome", "Get welcomed by the bot."));
		commandData.add(Commands.slash("roles", "Get all roles in guild."));
		event.getGuild().updateCommands().addCommands(commandData).queue();
		
//		if (event.getGuild().getIdLong() == 763404947500564500L) {
//			
//		}
	}

	@Override
	public void onGuildJoin(GuildJoinEvent event) {
		List<CommandData> commandData = new ArrayList<>();
		commandData.add(Commands.slash("welcome", "Get welcomed by the bot."));
		event.getGuild().updateCommands().addCommands(commandData).queue();
	}

	/**
	 * Global command -- up to an hour to update (unlimited)
	 */
//	@Override
//	public void onReady(ReadyEvent event) {
//		List<CommandData> commandData = new ArrayList<>();
//		commandData.add(Commands.slash("welcome", "Get welcomed by the bot."));
//		event.getJDA().updateCommands().addCommands(commandData).queue();
//	}
}
