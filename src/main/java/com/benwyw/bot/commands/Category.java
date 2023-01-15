package com.benwyw.bot.commands;

/**
 * Category that represents a group of similar commands.
 * Each category has a name and an emoji.
 *
 * @author Benwyw
 */
public enum Category {
    MUSIC(":musical_note:", "Music"),
    ECONOMY(":moneybag:", "Economy"),
    AUTOMATION(":gear:", "Automation"),
    UTILITY(":tools:", "Utility"),
    GREETINGS(":wave:", "Greetings"),
    SUGGESTIONS(":thought_balloon:", "Suggestions");

    public final String emoji;
    public final String name;

    Category(String emoji, String name) {
        this.emoji = emoji;
        this.name = name;
    }
}
