package com.benwyw.bot.commands;

import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;

public class RolesCommand {

	public RolesCommand(SlashCommandInteractionEvent event) {
		event.deferReply().queue();
		
		String response = "";
		for (Role role : event.getGuild().getRoles()){
			response += role.getAsMention() + "\n";
		};
		response += getTest();
		
		event.getHook().sendMessage(response).queue();
	}
	
	private String getTest() {
		return "test";
	}
}
