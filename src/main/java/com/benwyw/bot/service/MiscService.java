package com.benwyw.bot.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.benwyw.bot.config.DiscordProperties;
import com.benwyw.bot.config.MiscProperties;
import com.benwyw.bot.config.RateLimitInterceptor;
import com.benwyw.bot.data.Feature;
import com.benwyw.bot.data.MessageEmbedFile;
import com.benwyw.bot.mapper.MiscMapper;
import com.benwyw.bot.service.common.APIService;
import com.benwyw.util.embeds.EmbedColor;
import com.benwyw.util.embeds.EmbedUtils;
import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import me.dilley.MineStat;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

@Slf4j
@Service
public class MiscService {

	private final static String NA = "N/A";

	@Autowired
	private ShardManager shardManager;

	@Autowired
	private CacheManager cacheManager;

	@Autowired
	private MiscProperties miscProperties;

	@Autowired
	private DiscordProperties discordProperties;

	@Autowired
	private SwaggerService swaggerService;

	@Value("${version}")
	private String version;

	@Autowired
	private MiscMapper miscMapper;

	@Autowired
	private RateLimitInterceptor rateLimitInterceptor;

	@Autowired
	private MinecraftService minecraftService;

	@Autowired
	private APIService apiService;

	@Autowired
	private LogService logService;

	public final static String logChannelName = "FBenI.Logs";

	public String getTitle() {
		return "Ben-AI-Java";
	}

	@Cacheable(value = "userBaseCache", key = "'userBase'")
	public Integer getUserBase() {
		return shardManager.getGuilds().stream()
				.mapToInt(Guild::getMemberCount)
				.sum();

		// deprecated and disabled due to privileged intents
//		return Math.toIntExact(shardManager.getGuilds().stream()
//				.flatMap(guild -> guild.loadMembers().get().stream())
//				.map(member -> member.getId())
//				.distinct() // filter out duplicate members
//				.count()); // count the remaining distinct members
	}

	@Scheduled(fixedDelay = 3600000) // run every hour
	public void refreshUserBaseCache() {
		Cache cache = cacheManager.getCache("userBaseCache");
		cache.evict("userBase");
		getUserBase(); // call getUserBase to refresh the cache
	}

	/**
	 * Retrieves the user base data from the underlying data source without using the cache.
	 * This method temporarily disables the cache to force a cache miss, retrieves the data from the underlying data source,
	 * and puts the data back into the cache for subsequent calls to retrieve the data from the cache again.
	 * This method should be used sparingly, as it can be expensive to retrieve data from the underlying data source every time.
	 *
	 * @return the Integer object containing the user base data from the underlying data source
	 */
	public Integer getUserBaseWithoutCache() {
		Cache cache = cacheManager.getCache("userBaseCache"); // get the userBaseCache
		cache.clear(); // clear the cache to force a cache miss

		// retrieve the data from the underlying data source
		Integer userBase = getUserBase();

		// put the data back into the cache
		cache.put("userBase", userBase);

		return userBase;
	}

	public MessageEmbed validateJoinedServers() {
		return EmbedUtils.createDefault("This command is temporarily disabled.");
	}

	public MessageEmbed messageToOwner(MessageEmbed messageEmbed) {
		User owner = shardManager.retrieveApplicationInfo().complete().getOwner();
		owner.openPrivateChannel().flatMap(channel -> channel.sendMessageEmbeds(messageEmbed)).queue();
		return EmbedUtils.createSuccess("Operation completed.");
	}

	public MessageEmbed announce(SlashCommandInteractionEvent event) {
		MessageEmbed masterMessageEmbed = EmbedUtils.createSuccess("Operation completed.");
		try {
			OptionMapping title = event.getOption("title");
			OptionMapping content = event.getOption("content");
			OptionMapping image = event.getOption("image");
			OptionMapping minecraft = event.getOption("minecraft");

			if ((ObjectUtils.isEmpty(content) || StringUtils.isBlank(content.getAsString()) ) && (ObjectUtils.isEmpty(minecraft) || !minecraft.getAsBoolean())) {
				throw new RuntimeException("content is required when minecraft flag is false");
			}

			EmbedBuilder embedBuilder = new EmbedBuilder();
			List<MessageEmbed> messageEmbedList = new ArrayList<>();
			List<Long> targetChannelList = new ArrayList<>();

			if (!(minecraft != null && minecraft.getAsBoolean())) { // normal announcement
				if (title != null) {
					embedBuilder.setTitle(title.getAsString());
				} else {
					embedBuilder.setTitle("Announcement");
				}
				if (image != null) {
					embedBuilder.setThumbnail(image.getAsAttachment().getUrl());
				}
				embedBuilder.setDescription(content.getAsString());
				embedBuilder.setColor(EmbedColor.DEFAULT.color);
				embedBuilder.setFooter(String.valueOf(LocalDateTime.now(ZoneId.of("Asia/Hong_Kong"))));

				messageEmbedList.add(embedBuilder.build());
				targetChannelList = miscProperties.getAnnounce();
			}
			else { // is Minecraft
				MessageEmbed healthCheckMinecraftServerEmbed = getHealthCheckMinecraftServerEmbed();

				embedBuilder.setTitle(minecraftService.getMinecraftServerIp(), minecraftService.getMinecraftServerDynmap());
				embedBuilder.setDescription(String.format("Updated to %s", healthCheckMinecraftServerEmbed.getFields().get(3).getValue()));
				embedBuilder.setAuthor("Ben's Minecraft Server");
				embedBuilder.setFooter(String.valueOf(LocalDateTime.now(ZoneId.of("Asia/Hong_Kong"))));
				embedBuilder.setThumbnail("https://i.imgur.com/NssQKDi.png");
				embedBuilder.setColor(EmbedColor.DEFAULT.color);

				messageEmbedList.add(embedBuilder.build());
				messageEmbedList.add(healthCheckMinecraftServerEmbed);

				targetChannelList = miscProperties.getAnnounceMinecraft();
			}

			for (Long mainChannel : targetChannelList) {
				TextChannel textChannel = shardManager.getTextChannelById(mainChannel);
				if (textChannel != null) {
					for (MessageEmbed messageEmbed : messageEmbedList) {
						textChannel.sendMessageEmbeds(messageEmbed).queue();
					}
				} else {
					shardManager.getTextChannelById(discordProperties.getChannels().get("FBenI.Logs")).sendMessageEmbeds(EmbedUtils.createError(String.format("MiscService.announce: send to channelId %s failed", mainChannel))).queue();
				}
			}
		}
		catch(Exception e) {
			masterMessageEmbed = EmbedUtils.createError(String.format("Operation failed.\n%s", e));
		}

		return masterMessageEmbed;
	}

	/**
	 * Convert Swagger to Excel
	 * @param event
	 * @return
	 */
	public MessageEmbedFile swaggerToExcel(SlashCommandInteractionEvent event) {
		MessageEmbedFile messageEmbedFile = new MessageEmbedFile();
		String msgSuffix = String.format(", you can also visit https://%s/swagge", miscProperties.getDomainName());

		try {
			OptionMapping file = event.getOption("json");
			if (file != null) {
				String fileExt = file.getAsAttachment().getFileExtension();
				if (!"json".equals(fileExt)) {
					messageEmbedFile.setMessageEmbed(EmbedUtils.createError("Require JSON file that ends with .json, you can also visit "));
				}
				messageEmbedFile.setFile(swaggerService.generateExcelFromSwaggerJson(file.getAsAttachment().getProxy().downloadToFile(File.createTempFile("swagger", fileExt)).get()));
				messageEmbedFile.setMessageEmbed(EmbedUtils.createSuccess("Successfully generated"));
				messageEmbedFile.setFileName(swaggerService.getFileName());
			} else { // File is empty
				messageEmbedFile.setMessageEmbed(EmbedUtils.createError(String.format("File is empty%s", msgSuffix)));
			}
		} catch(Exception e) {
			logService.messageToLog(String.format("MiscService - swaggerToExcel:\n%s", e));
			messageEmbedFile.setMessageEmbed(EmbedUtils.createError(String.format("An unexpected error occurred%s", msgSuffix)));
		}

		return messageEmbedFile;
	}

	public String getVersion() {
		return version;
	}

	public IPage<Feature> getFeatures(int pageNumber, int limit) {
		Page<Feature> page = new Page<>(pageNumber, limit);
		page.setRecords(miscMapper.getFeatures(page));
		page.setTotal(miscMapper.getFeaturesCount());
		return page;
	}

	/**
	 * Remove IP Address from rate limit list
	 * @param event SlashCommandInteractionEvent
	 * @return MessageEmbed
	 */
	public MessageEmbed removeFromRequestsPerIp(SlashCommandInteractionEvent event) {
		MessageEmbed messageEmbed;
		String ipAddress = Objects.requireNonNull(event.getOption("ipaddress")).getAsString();
		if (StringUtils.isNotBlank(ipAddress)) {
			boolean isSuccessful = rateLimitInterceptor.removeFromRequestsPerIp(ipAddress);
			if (isSuccessful) {
				messageEmbed = EmbedUtils.createSuccess("Removed specified IP from rate limit list.");
			} else {
				messageEmbed = EmbedUtils.createError("Specified IP not found in rate limit list.");
			}
		}
		else {
			messageEmbed = EmbedUtils.createError("IP Address is empty");
		}
		return messageEmbed;
	}

	/**
	 * Command to send a private message to a user.
	 * Usage: /dm <userID> <message>
	 */
	@Deprecated
	public void sendPrivateMessage(SlashCommandInteractionEvent event) {
		String userId = event.getOption("userid").getAsString();
		String messageToSend = event.getOption("message").getAsString();

//		User user = event.getJDA().retrieveUserById(userId).queue();
		event.getJDA().retrieveUserById(userId).queue(user -> {
			System.out.println(user);
			if (user != null) {
				user.openPrivateChannel().queue((channel) -> {
					channel.sendMessage(messageToSend).queue();
				});
			} else {
				event.reply("User not found!").queue();
			}
		});

	}

	/**
	 * Command to delete message that contains specific keyword.
	 * @param event SlashCommandInteractionEvent
	 */
	@Deprecated
	public void deleteMessagesWithKeywordDeprecated(SlashCommandInteractionEvent event) {
		String channelId = event.getMessageChannel().getId();
		String keyword = event.getOption("keyword").getAsString();
		LocalDate startDate = LocalDate.parse(event.getOption("start_date").getAsString(), DateTimeFormatter.BASIC_ISO_DATE);

		TextChannel channel = shardManager.getTextChannelById(channelId);
		if (channel == null) {
			log.error("Channel not found for ID: " + channelId);
			return;
		}

		channel.getIterableHistory().takeAsync(100).thenAccept(messages -> {
			messages.stream()
					.filter(message -> message.getContentDisplay().toLowerCase().contains(keyword.toLowerCase()))
					.filter(message -> message.getTimeCreated().toLocalDate().isEqual(startDate) || message.getTimeCreated().toLocalDate().isAfter(startDate))
					.forEach(message -> {
						message.delete().queue(
								success -> log.info("Deleted message with ID: " + message.getId()),
								error -> log.error("Failed to delete message with ID: " + message.getId())
						);
					});
		}).exceptionally(throwable -> {
			log.error("Error retrieving messages from channel: " + channelId, throwable);
			return null;
		});
	}

	/**
	 * Command to delete message that contains specific keyword upon received 2FA confirmation.
	 * @param event SlashCommandInteractionEvent
	 */
	public String deleteMessagesWithKeyword(SlashCommandInteractionEvent event) {
		String channelId = event.getMessageChannel().getId();
		String keyword = event.getOption("keyword").getAsString();
		LocalDate startDate = LocalDate.parse(event.getOption("start_date").getAsString(), DateTimeFormatter.BASIC_ISO_DATE);
		String ownerId = event.getJDA().retrieveApplicationInfo().complete().getOwner().getId(); // Owner ID

		TextChannel channel = shardManager.getTextChannelById(channelId);
		if (channel == null) {
			log.error("Channel not found for ID: " + channelId);
			return "Channel not found for ID: " + channelId;
		}

		// Generate a random 5 capital letter code
		String code = new Random().ints(5, 'A', 'Z'+1)
				.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
				.toString();

		// Send a private message with the code to the owner
		User owner = shardManager.getUserById(ownerId);
		if (owner != null) {
			owner.openPrivateChannel().queue(privateChannel -> {
				privateChannel.sendMessage("Please reply with this code to confirm deletion: " + code).queue();

				// Create a list to hold your listeners
				List<ListenerAdapter> listeners = new ArrayList<>();

				// Create a listener for the owner's reply
				ListenerAdapter listener = new ListenerAdapter() {
					@Override
					public void onMessageReceived(@Nonnull MessageReceivedEvent e) {
						if (e.getAuthor().equals(owner) && e.getChannel().getType() == ChannelType.PRIVATE && e.getMessage().getContentRaw().equals(code)) {
							// Proceed with the deletion
							channel.getIterableHistory().takeAsync(100).thenAccept(messages -> {
								long count = messages.stream()
										.filter(message -> message.getContentDisplay().toLowerCase().contains(keyword.toLowerCase()))
										.filter(message -> message.getTimeCreated().toLocalDate().isEqual(startDate) || message.getTimeCreated().toLocalDate().isAfter(startDate))
										.peek(message -> {
											message.delete().queue(
													success -> log.info("Deleted message with ID: " + message.getId()),
													error -> log.error("Failed to delete message with ID: " + message.getId())
											);
										}).count();

								// Send a message indicating the number of messages deleted
								event.getHook().sendMessageEmbeds(EmbedUtils.createSuccess("Deleted " + count + " messages.")).queue();
							}).exceptionally(throwable -> {
								log.error("Error retrieving messages from channel: " + channelId, throwable);
								return null;
							});

							// Remove the listener after handling the event
							shardManager.removeEventListener(this);
							listeners.remove(this);
						}
					}
				};

				shardManager.addEventListener(listener);
				listeners.add(listener);

				// Schedule a task to remove the listener after 5 minutes
				new java.util.Timer().schedule(
						new java.util.TimerTask() {
							@Override
							public void run() {
								// Only remove the listener if it is in your list
								if (listeners.contains(listener)) {
									shardManager.removeEventListener(listener);
									listeners.remove(listener);

									// Send a message indicating request is expired
									event.getHook().sendMessageEmbeds(EmbedUtils.createError("Deletion request expired.")).queue();
								}
							}
						},
						1 * 60 * 1000
				);
			});
		} else {
			log.error("Owner not found for ID: " + ownerId);
			return "Owner not found for ID: " + ownerId;
		}

		return "Deletion request initiated.";
	}

	public MessageEmbed getHealthCheckMinecraftServerEmbed() {
		MineStat mineStat = minecraftService.getServerStatus();
		String discordSRVStatus = shardManager.retrieveUserById(minecraftService.getDiscordUserDiscordSRV()).complete().getJDA().getPresence().getStatus().name();

		String minecraftServerDynmap = minecraftService.getMinecraftServerDynmap();
		boolean isAccessible = apiService.isAccessible(minecraftServerDynmap);

		EmbedBuilder embedBuilder = new EmbedBuilder();
		embedBuilder.setTitle(mineStat.getAddress(), minecraftServerDynmap);
		embedBuilder.setAuthor("Ben's Minecraft Server");
		embedBuilder.setFooter(String.valueOf(LocalDateTime.now(ZoneId.of("Asia/Hong_Kong"))));
		embedBuilder.setThumbnail("https://i.imgur.com/NssQKDi.png");
		embedBuilder.addField("Server", mineStat.isServerUp() ? EmbedUtils.GREEN_TICK : EmbedUtils.RED_X, true);
		embedBuilder.addField("Discord", StringUtils.isNotBlank(discordSRVStatus) && "ONLINE".equalsIgnoreCase(discordSRVStatus) ? EmbedUtils.GREEN_TICK : EmbedUtils.RED_X, true);
		embedBuilder.addField("Map", isAccessible ? EmbedUtils.GREEN_TICK : EmbedUtils.RED_X, true);
		embedBuilder.addField("Version", mineStat.isServerUp() ? mineStat.getVersion() : NA, true);
		embedBuilder.addField("Players", mineStat.isServerUp() ? String.format("%s/%s", mineStat.getCurrentPlayers(), mineStat.getMaximumPlayers()) : NA, true);
//		embedBuilder.addField("Latency", mineStat.isServerUp() ? String.valueOf(mineStat.getLatency()) : NA, true);
		embedBuilder.setColor(mineStat.isServerUp() ? EmbedColor.SUCCESS.color : EmbedColor.ERROR.color);
		return embedBuilder.build();
	}

}
