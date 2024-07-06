package com.benwyw.bot;

import com.benwyw.bot.commands.CommandRegistry;
import com.benwyw.bot.data.GuildData;
import com.benwyw.bot.listeners.ButtonListener;
import com.benwyw.bot.listeners.CommandListener;
import com.benwyw.bot.listeners.MessageListener;
import com.benwyw.bot.listeners.MusicListener;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.sql.DataSource;
import java.net.UnknownHostException;

@Slf4j
@Configuration
@EnableConfigurationProperties
@EnableScheduling
@EnableCaching
@EnableAsync
//@PropertySource("classpath:application.properties")
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class Main {

//	@Configuration
//	@EnableAsync
//	public class AsyncConfiguration implements AsyncConfigurer {
//		@Bean(name = "asyncExecutor")
//		public Executor getAsyncExecutor() {
//			ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//			executor.setCorePoolSize(10);
//			executor.setMaxPoolSize(20);
//			executor.setQueueCapacity(100);
//			executor.setThreadNamePrefix("AsyncExecutor-");
//			executor.initialize();
//			return executor;
//		}
//
//		// Other overridden methods if needed
//	}

//	public final @NotNull Dotenv config;
//	public final @NotNull ShardManager shardManager;
//	public final @NotNull MusicListener musicListener;
//	
//	public Main() throws LoginException {
//		config = Dotenv.configure().load();
//		String token = config.get("TOKEN");
//		
//		DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.createDefault(token);
//		builder.setStatus(OnlineStatus.ONLINE);
//		builder.setActivity(Activity.watching("音樂幫到你"));
//		builder.enableIntents(GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES);
//		builder.addEventListeners(new CommandRegistry(this));
//		shardManager = builder.build();
//		GuildData.init(this);
//		
//		musicListener = new MusicListener(config.get("SPOTIFY_CLIENT_ID"), config.get("SPOTIFY_TOKEN"));
//		shardManager.addEventListener(
//				new CommandListener(),
//				musicListener);
//	}

	// -------------

	public final @NotNull Dotenv config = Dotenv.configure().load();
	public @NotNull ShardManager shardManager;
	
	@Qualifier("musicListener")
	public final @NotNull MusicListener musicListener = new MusicListener(config.get("SPOTIFY_CLIENT_ID"), config.get("SPOTIFY_TOKEN"));

	/**
	 * JDA
	 * @param commandListener CommandListener
	 * @param buttonListener ButtonListener
	 * @param messageListener MessageListener
	 * @return ShardManager
	 */
	@Bean
	ShardManager shardManager(@Qualifier("commandListener") final CommandListener commandListener,
			@Qualifier("buttonListener") final ButtonListener buttonListener,
			@Qualifier("messageListener") final MessageListener messageListener) {
		log.info("Inside JDA");
		final DefaultShardManagerBuilder builder = DefaultShardManagerBuilder.createDefault(config.get("TOKEN"));
		builder.setStatus(OnlineStatus.ONLINE);
		builder.setActivity(Activity.watching("音樂幫到你"));

		//disable all intent
//		builder.enableIntents(GatewayIntent.GUILD_MEMBERS); // GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_PRESENCES

		builder.addEventListeners(new CommandRegistry(this));
//		builder.setMemberCachePolicy(MemberCachePolicy.ALL);
//		builder.disableCache(CacheFlag.ACTIVITY, CacheFlag.CLIENT_STATUS,
//				CacheFlag.MEMBER_OVERRIDES);
//		builder.setChunkingFilter(ChunkingFilter.NONE);
//		builder.disableIntents(GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_MESSAGE_TYPING);
//		builder.setLargeThreshold(50);
		try {
			final ShardManager shardManager = builder.build();
			GuildData.init(this);
			shardManager.addEventListener(commandListener, musicListener, buttonListener, messageListener);
			
//			final JDA jda = builder.build();
			log.info("Inside GuildData");
			
//			jda.awaitReady();
//			jda.getGuildById(Properties.GUILD_ROBLOX_DISCORD_ID).updateCommands()
//			   .addCommands(new CommandData(SlashCommandConstants.COMMAND_HELP, "Shows all current commands"))
//			   .addCommands(new CommandData(SlashCommandConstants.COMMAND_ABOUT, "About the bot"))
//			   .addCommands(new CommandData(SlashCommandConstants.COMMAND_USER_INFO, "Shows information about the user").addOption(OptionType.USER, "user", "The user to see the info", true))
//			   .addCommands(new CommandData(SlashCommandConstants.COMMAND_ROBLOX_USER_INFO, "Shows information about the Roblox user").addOption(OptionType.STRING, "username", "The username to see the info", true))
//			   //.addCommands(new CommandData(SlashCommandConstants.COMMAND_SCAN_URL, "Checks URL for viruses").addOption(OptionType.STRING, "url", "The url to see the info", true))
//			   .queue();
			return shardManager;
		} catch (final Exception e) {
			log.error(String.valueOf(e));
		}
//		} catch (final LoginException e) {
//			TelegramService.sendToTelegram(Instant.now(), TelegramService.ERROR_UNKNOWN);
//		} catch (final InterruptedException e) {
//			TelegramService.sendToTelegram(Instant.now(), TelegramService.ERROR_WAIT_JDA);
//		}
		return null;
	}

	@Bean
	public DataSource dataSource() {

		// Create a DataSource object and set its properties
		Dotenv config = Dotenv.configure().load();

		DataSource dataSource = DataSourceBuilder.create()
				.driverClassName("oracle.jdbc.OracleDriver")
				.url(config.get("SPRING_DATASOURCE_URL"))
				.username(config.get("ORACLE_DB_USER"))
				.password(config.get("ORACLE_DB_PASSWORD"))
				.build();

		// Return the DataSource object
		return dataSource;
	}

//	@Bean
//	TelegramService telegramService(@Value("${telegram.token}") final String telegramToken,
//			@Value("${developer.update-notes}") final String devNote) {
//		return new TelegramService(telegramToken, devNote);
//	}

	/**
	 * Mongo template.
	 *
	 * @return the mongo template
	 * @throws UnknownHostException the unknown host exception
	 */
//	@Bean
//	MongoTemplate mongoTemplate() throws UnknownHostException {
//		return new MongoTemplate(MongoClients.create("mongodb://localhost:27017"), "rdss");
//	}

//	@PreDestroy
//	public void onExit() {
		// TelegramService.sendToTelegram(Instant.now(), TelegramService.CDS_END);
//	}
	
	// -------------
	
    public static void main( String[] args ) {
//    	try {
//    		Main bot = new Main();
		SpringApplication.run(Main.class, args);
    		log.info("Bot started.");
//    	} catch (LoginException e) {
//    		log.error("Provided bot token is invalid!");
//    	}
    }
}
