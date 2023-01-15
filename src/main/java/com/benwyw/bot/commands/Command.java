package com.benwyw.bot.commands;

import java.util.ArrayList;
import java.util.List;

import com.benwyw.bot.Main;
import com.benwyw.bot.commands.Category;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

public abstract class Command {
	public Main bot;
    public String name;
    public String description;
    public Category category;
    public List<OptionData> args;
    public List<SubcommandData> subCommands;
    public Permission permission; //Permission user needs to execute this command
    public Permission botPermission; //Permission bot needs to execute this command

    public Command(Main bot) {
        this.bot = bot;
        this.args = new ArrayList<>();
        this.subCommands = new ArrayList<>();
    }

    public abstract void execute(SlashCommandInteractionEvent event);
}
