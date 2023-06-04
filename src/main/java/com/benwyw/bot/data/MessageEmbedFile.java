package com.benwyw.bot.data;

import lombok.Data;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.io.File;

@Data
public class MessageEmbedFile {

	private MessageEmbed messageEmbed;
	private File file;
	private String fileName;
}
