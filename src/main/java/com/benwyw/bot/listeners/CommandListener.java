package com.benwyw.bot.listeners;

import com.benwyw.bot.SpringContext;
import com.benwyw.bot.commands.CommandRegistry;
import com.benwyw.bot.commands.RolesCommand;
import com.benwyw.bot.data.GuildData;
import com.benwyw.bot.service.MiscService;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
public class CommandListener extends ListenerAdapter {

	@Override
	public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
		String command = event.getName();

		switch (command) {
			case "welcome" -> {
				String userTag = event.getUser().getAsTag();
				event.reply(String.format("WELCOME **%s**!", userTag)).queue();
			}
			case "roles" -> new RolesCommand(event);
		}
	}

	/**
	 * Guild command -- instantly updated (max 100)
	 */
	@Profile("local")
	@Override
	public void onGuildReady(GuildReadyEvent event) {
//		List<CommandData> commandData = new ArrayList<>();
//		commandData.add(Commands.slash("welcome", "Get welcomed by the bot."));
//		commandData.add(Commands.slash("roles", "Get all roles in guild."));
//		event.getGuild().updateCommands().addCommands(commandData).queue();
		
//		if (event.getGuild().getIdLong() == 763404947500564500L) {
//			
//		}

		// Clear Global registered command, avoid duplications
//		event.getJDA().updateCommands().addCommands(new ArrayList<>()).queue(succ -> {}, fail -> {});

		MiscService miscService = SpringContext.getBean(MiscService.class);
		if (miscService.validateJoinedServers(event.getGuild())) {
			GuildData.get(event.getGuild());
			event.getGuild().updateCommands().addCommands(CommandRegistry.unpackCommandData()).queue();
		}
	}

	@Profile("local")
	@Override
	public void onGuildJoin(@NotNull GuildJoinEvent event) {
//		List<CommandData> commandData = new ArrayList<>();
//		commandData.add(Commands.slash("welcome", "Get welcomed by the bot."));
//		event.getGuild().updateCommands().addCommands(commandData).queue();
		MiscService miscService = SpringContext.getBean(MiscService.class);
		if (miscService.validateJoinedServers(event.getGuild())) {
			GuildData.get(event.getGuild());
			event.getGuild().updateCommands().addCommands(CommandRegistry.unpackCommandData()).queue();
		}
	}

	/**
	 * Global command -- up to an hour to update (unlimited)
	 */
	@Profile("!local")
	@Override
	public void onReady(ReadyEvent event) {
//		List<CommandData> commandData = new ArrayList<>();
//		commandData.add(Commands.slash("welcome", "Get welcomed by the bot."));
//		event.getJDA().updateCommands().addCommands(commandData).queue();

		// Register global slash commands
		event.getJDA().updateCommands().addCommands(CommandRegistry.unpackCommandData()).queue(succ -> {}, fail -> {});
	}
}
