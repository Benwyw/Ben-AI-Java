package com.benwyw.bot.service;

import com.benwyw.bot.data.Points;
import com.benwyw.bot.mapper.LoopMapper;
import com.benwyw.util.embeds.EmbedUtils;
import com.benwyw.bot.config.DiscordProperties;
import com.benwyw.bot.config.LoopProperties;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class LoopService {

	@Autowired
	@SuppressWarnings("unused")
	private ShardManager shardManager;

	@Autowired
	private DiscordProperties discordProperties;

	@Autowired
	private LoopProperties loopProperties;

	@Autowired
	private LoopMapper loopMapper;

	@Autowired
	private MiscService miscService;

	/**
	 * milliseconds
	 * 1 hour = 3600000
	 * 1 min = 60000
	 */
	@Scheduled(fixedRate = 3600000)
	public void performTask() throws IOException {
		List<MessageEmbed> bitDefenderList = getBitdefender();
		if (bitDefenderList != null && !bitDefenderList.isEmpty()) {
			for (MessageEmbed bitDefender : bitDefenderList) {
//				shardManager.getTextChannelById(discordProperties.getChannels().get("FBenI.Security")).sendMessageEmbeds(bitDefender).queue(); // test channel

				for (Long loopChannels : loopProperties.getBitdefender()) {
					TextChannel textChannel = shardManager.getTextChannelById(loopChannels);
					if (textChannel != null) {
						shardManager.getTextChannelById(loopChannels).sendMessageEmbeds(bitDefender).queue();
					}
					else {
						miscService.messageToLog(String.format("LoopService.performTask: send to channelId %s failed", loopChannels), false);

					}
				}
			}
		}
	}

	public List<MessageEmbed> getBitdefender() throws IOException {
		/**
		 * Read from URL
		 */
		URL url = new URL("https://www.bitdefender.com/blog/api/rss/hotforsecurity/industry-news/");
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");

		int responseCode = connection.getResponseCode();
		if (responseCode >= 200 && responseCode <= 299) {
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String line;
			StringBuilder content = new StringBuilder();
			while ((line = reader.readLine()) != null) {
				content.append(line);
			}
			reader.close();

			// Dummy data
			//		String content = "<rss version=\"2.0\"    xmlns:dc=\"http://purl.org/dc/elements/1.1/\"    xmlns:content=\"http://purl.org/rss/1.0/modules/content/\"    xmlns:atom=\"http://www.w3.org/2005/Atom\"    xmlns:media=\"http://search.yahoo.com/mrss/\">    <channel><title>Consumer Insights</title><description>News, views and insights from the Bitdefender experts</description><link>https://www.bitdefender.com/blog/hotforsecurity/</link><image><url>https://download.bitdefender.com/resources/images/favicon/favicon-32x32.png</url><title>Consumer Insights</title><link>https://www.bitdefender.com/blog/hotforsecurity/</link></image><generator>Bitdefender Blog</generator><lastBuildDate>Sun, 16 Apr 2023 08:45:52 GMT</lastBuildDate><atom:link href=\"https://www.bitdefender.com/blog/api/rss/hotforsecurity/industry-news/\" rel=\"self\" type=\"application/rss+xml\"/><ttl>3600</ttl><item><title>Attackers Compromise Website to Display Fake Chrome Updates and Deploy Cryptominer</title><description><![CDATA[Hackers have found a new way to trick people into downloading and installingmalware: convince them that their Google Chrome browser is outdated and needs amanual update.Remotely compromising a device or software requires a lot of technicalknowledge. But what if hackers could persuade people to compromise their owndevices? That takes a lot less work and not nearly as much technical expertise.According to a TechRadar report[https://www.techradar.com/news/hackers-have-been-spreading-malwar]]></description><link>https://www.bitdefender.com/blog/hotforsecurity/attackers-compromise-website-to-display-fake-chrome-updates-and-deploy-cryptominer/</link><guid isPermaLink=\"false\">6437ec91ec6c331524f3707b</guid><category><![CDATA[Industry News]]></category><dc:creator>Silviu STAHIE</dc:creator><pubDate>Thu, 13 Apr 2023 11:51:38 GMT</pubDate><media:content url=\"https://blogapp.bitdefender.com/hotforsecurity/content/images/2023/04/bitcoin-g474013081_1920.jpg\" medium=\"image\"/><content:encoded><![CDATA[Hackers have found a new way to trick people into downloading and installingmalware: convince them that their Google Chrome browser is outdated and needs amanual update.Remotely compromising a device or software requires a lot of technicalknowledge. But what if hackers could persuade people to compromise their owndevices? That takes a lot less work and not nearly as much technical expertise.According to a TechRadar report[https://www.techradar.com/news/hackers-have-been-spreading-malwar]]></content:encoded></item><item><title>US Seeks to Enforce Stricter Safety Testing of AI Tools</title><description><![CDATA[The US government has unveiled plans to implement more rigorous safety measuresfor testing artificial intelligence (AI) tools such as ChatGPT before they’rereleased for public access.Reportedly, the White House still needs to decide the extent and manner ofgovernment involvement, if any. However, addressing risks and consumer concernsseems to be a key motivator of this decision.On Tuesday, the Commerce Department disclosed its intention to allocate the next60 days to analyze the possibi]]></description><link>https://www.bitdefender.com/blog/hotforsecurity/us-seeks-to-enforce-stricter-safety-testing-of-ai-tools/</link><guid isPermaLink=\"false\">6437db6dec6c331524f37070</guid><category><![CDATA[Industry News]]></category><dc:creator>Vlad CONSTANTINESCU</dc:creator><pubDate>Thu, 13 Apr 2023 10:39:29 GMT</pubDate><media:content url=\"https://blogapp.bitdefender.com/hotforsecurity/content/images/2023/04/tabrez-syed-dbc9DGSJzKo-unsplash.jpg\" medium=\"image\"/><content:encoded><![CDATA[The US government has unveiled plans to implement more rigorous safety measuresfor testing artificial intelligence (AI) tools such as ChatGPT before they’rereleased for public access.Reportedly, the White House still needs to decide the extent and manner ofgovernment involvement, if any. However, addressing risks and consumer concernsseems to be a key motivator of this decision.On Tuesday, the Commerce Department disclosed its intention to allocate the next60 days to analyze the possibi]]></content:encoded></item><item><title>Attackers Using Public USB Outlets to Spread Malware, FBI Warns</title><description><![CDATA[The FBI has warned that charging your phone via a USB cable from a free chargingstation could be the worst decision you ever make regarding security.We often advise people to avoid free Wi-Fi networks or at least use a VPNsolution when connecting to an unknown Wi-Fi, as attackers can control wirelessnetworks to capture all traffic from the victims' devices. But connecting yourphone to an unknown outlet that supposedly provides free charging is infinitelyworse.Some businesses, such as ai]]></description><link>https://www.bitdefender.com/blog/hotforsecurity/attackers-using-public-usb-outlets-to-spread-malware-fbi-warns-2/</link><guid isPermaLink=\"false\">6436a1f6ec6c331524f36fdf</guid><category><![CDATA[Industry News]]></category><dc:creator>Silviu STAHIE</dc:creator><pubDate>Wed, 12 Apr 2023 12:21:38 GMT</pubDate><media:content url=\"https://blogapp.bitdefender.com/hotforsecurity/content/images/2023/04/power-outlet-g412cfd56d_1920.jpg\" medium=\"image\"/><content:encoded><![CDATA[The FBI has warned that charging your phone via a USB cable from a free chargingstation could be the worst decision you ever make regarding security.We often advise people to avoid free Wi-Fi networks or at least use a VPNsolution when connecting to an unknown Wi-Fi, as attackers can control wirelessnetworks to capture all traffic from the victims' devices. But connecting yourphone to an unknown outlet that supposedly provides free charging is infinitelyworse.Some businesses, such as ai]]></content:encoded></item><item><title>QuaDream ‘Reign’ Spyware Used to Hack iPhones of High-Profile Targets</title><description><![CDATA[Security researchers have discovered new evidence of spyware targeting Applesmartphones during the vulnerable days of iOS 14, dating back to 2021.In a report published this week, Citizen Labresearchers of the University ofToronto identified at least five civil society victims infected with ‘Reign’spyware developed by Israeli firm QuaDream.Targets included journalists, political opposition figures, and a non-governmentorganisation worker, in North America, Central Asia, Southeast Asia, Eu]]></description><link>https://www.bitdefender.com/blog/hotforsecurity/quadream-reign-spyware-used-to-hack-iphones-of-high-profile-targets/</link><guid isPermaLink=\"false\">64369de2ec6c331524f36fc5</guid><category><![CDATA[Industry News]]></category><dc:creator>Filip TRUȚĂ</dc:creator><pubDate>Wed, 12 Apr 2023 12:14:53 GMT</pubDate><media:content url=\"https://blogapp.bitdefender.com/hotforsecurity/content/images/2023/04/towfiqu-barbhuiya-em5w9_xj3uU-unsplash.jpg\" medium=\"image\"/><content:encoded><![CDATA[Security researchers have discovered new evidence of spyware targeting Applesmartphones during the vulnerable days of iOS 14, dating back to 2021.In a report published this week, Citizen Labresearchers of the University ofToronto identified at least five civil society victims infected with ‘Reign’spyware developed by Israeli firm QuaDream.Targets included journalists, political opposition figures, and a non-governmentorganisation worker, in North America, Central Asia, Southeast Asia, Eu]]></content:encoded></item><item><title>Kodi data breach exposes info and private messages of 400,000 users</title><description><![CDATA[A data breach at The Kodi Foundation forum has exposed the personal info of over400,000 users.The non-profit organization is the developer of the Kodi media center, a freeand open-source software entertainment hub and media player.According to a breach notice published April 8, the Kodi Team learned ofunauthorized access after a data dump of its forum user base (MyBB) was offeredfor sale online.Kodi’s post also revealed how the criminals used compromised admin credentialsto infiltrate]]></description><link>https://www.bitdefender.com/blog/hotforsecurity/kodi-data-breach-exposes-info-and-private-messages-of-400-000-users/</link><guid isPermaLink=\"false\">64368e55ec6c331524f36fb3</guid><category><![CDATA[Digital Privacy]]></category><category><![CDATA[Industry News]]></category><dc:creator>Alina BÎZGĂ</dc:creator><pubDate>Wed, 12 Apr 2023 11:03:25 GMT</pubDate><media:content url=\"https://blogapp.bitdefender.com/hotforsecurity/content/images/2023/04/Kodi-data-breach-exposes-info-and-private-messages-of-400-000-users.jpg\" medium=\"image\"/><content:encoded><![CDATA[A data breach at The Kodi Foundation forum has exposed the personal info of over400,000 users.The non-profit organization is the developer of the Kodi media center, a freeand open-source software entertainment hub and media player.According to a breach notice published April 8, the Kodi Team learned ofunauthorized access after a data dump of its forum user base (MyBB) was offeredfor sale online.Kodi’s post also revealed how the criminals used compromised admin credentialsto infiltrate]]></content:encoded></item><item><title>OpenAI Unveils New Bug Bounty Program to Fortify Cybersecurity</title><description><![CDATA[To strengthen the security of its state-of-the-art line of products, OpenAI haslaunched a novel bug bounty program, inviting registered security specialists toidentify and report potential system flaws.The program boasts attractive incentives, starting at $200 for less significantbugs and reaching an impressive $20,000 for critical vulnerabilities.OpenAI said its latest initiative signifies its dedication to cybersecurity asthe organization acknowledges the hazards associated with the sw]]></description><link>https://www.bitdefender.com/blog/hotforsecurity/openai-unveils-new-bug-bounty-program-to-fortify-cybersecurity/</link><guid isPermaLink=\"false\">64367e79ec6c331524f36fa5</guid><category><![CDATA[Industry News]]></category><dc:creator>Vlad CONSTANTINESCU</dc:creator><pubDate>Wed, 12 Apr 2023 09:49:12 GMT</pubDate><media:content url=\"https://blogapp.bitdefender.com/hotforsecurity/content/images/2023/04/ilgmyzin-agFmImWyPso-unsplash.jpg\" medium=\"image\"/><content:encoded><![CDATA[To strengthen the security of its state-of-the-art line of products, OpenAI haslaunched a novel bug bounty program, inviting registered security specialists toidentify and report potential system flaws.The program boasts attractive incentives, starting at $200 for less significantbugs and reaching an impressive $20,000 for critical vulnerabilities.OpenAI said its latest initiative signifies its dedication to cybersecurity asthe organization acknowledges the hazards associated with the sw]]></content:encoded></item><item><title>Google to Stop Money Lending Apps from Collecting Personal Information from Users</title><description><![CDATA[Google will update its rules for personal loan apps on Android devices aftersome companies collected personal data from users, making it easier to forcethem to pay.Some money lenders go to extreme lengths when trying to collect debts fromclients. One of the measures was to collect information from the people'sdevices, including accessing call logs, contacts, videos, photos and evenlocation.\"We don't allow apps that expose users to deceptive or harmful financialproducts and services,\" s]]></description><link>https://www.bitdefender.com/blog/hotforsecurity/google-to-stop-money-lending-apps-from-collecting-personal-information-from-users/</link><guid isPermaLink=\"false\">6435691fec6c331524f36f98</guid><category><![CDATA[Industry News]]></category><dc:creator>Silviu STAHIE</dc:creator><pubDate>Tue, 11 Apr 2023 14:07:25 GMT</pubDate><media:content url=\"https://blogapp.bitdefender.com/hotforsecurity/content/images/2023/04/apps-gcc2f213a6_1920.jpg\" medium=\"image\"/><content:encoded><![CDATA[Google will update its rules for personal loan apps on Android devices aftersome companies collected personal data from users, making it easier to forcethem to pay.Some money lenders go to extreme lengths when trying to collect debts fromclients. One of the measures was to collect information from the people'sdevices, including accessing call logs, contacts, videos, photos and evenlocation.\"We don't allow apps that expose users to deceptive or harmful financialproducts and services,\" s]]></content:encoded></item><item><title>Personal data of fast food workers stolen in Yum! Brands ransomware attack</title><description><![CDATA[Yum! Brands, which owns KFC, Pizza Hut, Taco Bell and The Habit Burger Grillfast food restaurants, is notifying an undisclosed number of employees of a databreach.According to a breach notification letter, the personal information ofindividuals was stolen during the mid-January ransomware attack[https://www.bitdefender.com/blog/hotforsecurity/ransomware-attack-hit-kfc-and-pizza-hut-stores-in-the-uk/] that forced nearly 300 restaurants to close down.Compromised data included personally i]]></description><link>https://www.bitdefender.com/blog/hotforsecurity/personal-data-of-fast-food-workers-stolen-in-yum-brands-ransomware-attack/</link><guid isPermaLink=\"false\">64354876ec6c331524f36f88</guid><category><![CDATA[Digital Privacy]]></category><category><![CDATA[Industry News]]></category><dc:creator>Alina BÎZGĂ</dc:creator><pubDate>Tue, 11 Apr 2023 11:51:26 GMT</pubDate><media:content url=\"https://blogapp.bitdefender.com/hotforsecurity/content/images/2023/04/Personal-data-of-fast-food-workers-stolen-in-Yum--Brands-ransomware-attack--2-.jpg\" medium=\"image\"/><content:encoded><![CDATA[Yum! Brands, which owns KFC, Pizza Hut, Taco Bell and The Habit Burger Grillfast food restaurants, is notifying an undisclosed number of employees of a databreach.According to a breach notification letter, the personal information ofindividuals was stolen during the mid-January ransomware attack[https://www.bitdefender.com/blog/hotforsecurity/ransomware-attack-hit-kfc-and-pizza-hut-stores-in-the-uk/] that forced nearly 300 restaurants to close down.Compromised data included personally i]]></content:encoded></item><item><title>US Charges Estonian Man with Procuring Electronics and Cybersecurity Tools for Russia</title><description><![CDATA[Estonian national Andrey Shevlyakov has been indicted on 18 counts of conspiracyand other charges in the United States for allegedly helping the Russiangovernment and military procure US-made electronics.Authorities apprehended the 45-year-old suspect in Tallinn, Estonia, on March28; if pronounced guilty, Shevlyakov could spend 20 years in prison.Allegedly, Shevlyakov acted as the middleman between various US electronicssuppliers and the Russian government, acquiring sensitive technology]]></description><link>https://www.bitdefender.com/blog/hotforsecurity/us-charges-estonian-man-with-procuring-electronics-and-cybersecurity-tools-for-russia/</link><guid isPermaLink=\"false\">64351c04ec6c331524f36f78</guid><category><![CDATA[Industry News]]></category><dc:creator>Vlad CONSTANTINESCU</dc:creator><pubDate>Tue, 11 Apr 2023 08:37:48 GMT</pubDate><media:content url=\"https://blogapp.bitdefender.com/hotforsecurity/content/images/2023/04/prison-553836_1920.jpg\" medium=\"image\"/><content:encoded><![CDATA[Estonian national Andrey Shevlyakov has been indicted on 18 counts of conspiracyand other charges in the United States for allegedly helping the Russiangovernment and military procure US-made electronics.Authorities apprehended the 45-year-old suspect in Tallinn, Estonia, on March28; if pronounced guilty, Shevlyakov could spend 20 years in prison.Allegedly, Shevlyakov acted as the middleman between various US electronicssuppliers and the Russian government, acquiring sensitive technology]]></content:encoded></item><item><title>Got an Older iPhone? Update to iOS 15.7.5 Now</title><description><![CDATA[Just days after patching two nasty security bugs in iPhones and Macs[https://www.bitdefender.com/blog/hotforsecurity/hackers-exploiting-two-new-zero-days-in-ios-16-and-macos-ventura-patch-now/], Apple is now rolling out separate updates to patch older hardware against theflaws. Users are strongly encouraged to install the updates sooner rather thanlater.CVE-2023-28206 and CVE-2023-28205 were reported by Clément Lecigne of Google'sThreat Analysis Group and Donncha Ó Cearbhaill of Amnesty I]]></description><link>https://www.bitdefender.com/blog/hotforsecurity/got-an-older-iphone-update-to-ios-15-7-5-now/</link><guid isPermaLink=\"false\">643514b4ec6c331524f36f6a</guid><category><![CDATA[Industry News]]></category><dc:creator>Filip TRUȚĂ</dc:creator><pubDate>Tue, 11 Apr 2023 08:05:34 GMT</pubDate><media:content url=\"https://blogapp.bitdefender.com/hotforsecurity/content/images/2023/04/iphone-g8673eb770_1280.jpg\" medium=\"image\"/><content:encoded><![CDATA[Just days after patching two nasty security bugs in iPhones and Macs[https://www.bitdefender.com/blog/hotforsecurity/hackers-exploiting-two-new-zero-days-in-ios-16-and-macos-ventura-patch-now/], Apple is now rolling out separate updates to patch older hardware against theflaws. Users are strongly encouraged to install the updates sooner rather thanlater.CVE-2023-28206 and CVE-2023-28205 were reported by Clément Lecigne of Google'sThreat Analysis Group and Donncha Ó Cearbhaill of Amnesty I]]></content:encoded></item><item><title>Long-Lasting Balada Injector Campaign Hits WordPress Websites, Researcher Reveals</title><description><![CDATA[A sweeping, long-lasting malicious campaign dubbed \"Balada Injector\" hascompromised an estimated 1 million WordPress websites since its inception in2017.The campaign leverages \"all known and recently discovered theme and pluginvulnerabilities\" to inject a Linux backdoor that lets attackers gainunauthorized access to affected websites. The campaign’s primary objectiveappears to be redirecting users to fraudulent tech support pages, fake lotterywins, and push notification scams.According]]></description><link>https://www.bitdefender.com/blog/hotforsecurity/long-lasting-balada-injector-campaign-hits-wordpress-websites-researcher-reveals/</link><guid isPermaLink=\"false\">6433de2eec6c331524f36f38</guid><category><![CDATA[Industry News]]></category><dc:creator>Vlad CONSTANTINESCU</dc:creator><pubDate>Mon, 10 Apr 2023 10:00:41 GMT</pubDate><media:content url=\"https://blogapp.bitdefender.com/hotforsecurity/content/images/2023/04/syringe-417786_1920.jpg\" medium=\"image\"/><content:encoded><![CDATA[A sweeping, long-lasting malicious campaign dubbed \"Balada Injector\" hascompromised an estimated 1 million WordPress websites since its inception in2017.The campaign leverages \"all known and recently discovered theme and pluginvulnerabilities\" to inject a Linux backdoor that lets attackers gainunauthorized access to affected websites. The campaign’s primary objectiveappears to be redirecting users to fraudulent tech support pages, fake lotterywins, and push notification scams.According]]></content:encoded></item><item><title>Hackers Exploiting Two New Zero-Days in iOS 16 and macOS Ventura - Patch Now!</title><description><![CDATA[Apple has issued out-of-band updates for iOS and macOS to address two newlydiscovered security flaws that criminals are said to be exploiting in the wild.Two weeks after patching ‘actively exploited[https://www.bitdefender.com/blog/hotforsecurity/apple-patches-actively-exploited-security-flaw-in-older-iphones-with-ios-15-7-4/]’ vulnerabilities in older iPhone models, Apple is now rolling out more securityupdates, this time to patch newer iterations against freshly discovered bugs -includi]]></description><link>https://www.bitdefender.com/blog/hotforsecurity/hackers-exploiting-two-new-zero-days-in-ios-16-and-macos-ventura-patch-now/</link><guid isPermaLink=\"false\">6433c97aec6c331524f36f2c</guid><category><![CDATA[Industry News]]></category><dc:creator>Filip TRUȚĂ</dc:creator><pubDate>Mon, 10 Apr 2023 08:32:41 GMT</pubDate><media:content url=\"https://blogapp.bitdefender.com/hotforsecurity/content/images/2023/04/apple-store.jpg\" medium=\"image\"/><content:encoded><![CDATA[Apple has issued out-of-band updates for iOS and macOS to address two newlydiscovered security flaws that criminals are said to be exploiting in the wild.Two weeks after patching ‘actively exploited[https://www.bitdefender.com/blog/hotforsecurity/apple-patches-actively-exploited-security-flaw-in-older-iphones-with-ios-15-7-4/]’ vulnerabilities in older iPhone models, Apple is now rolling out more securityupdates, this time to patch newer iterations against freshly discovered bugs -includi]]></content:encoded></item><item><title>Attackers Use Old YouTube Feature in Phishing Attack to Send Messages from Legitimate Email</title><description><![CDATA[A new phishing campaign using a YouTube feature that helps attackers send emailsfrom a valid email has been making the rounds. Google is already investigatingthe issue, and our telemetry shows the phishing campaign is live.Spam filters can quickly determine if an email is part of a phishing campaign,and one way to do that is by looking at the email address that sent the message.While a spoofed address might trick a user by looking like a real one, theserver won’t make the same mistake.N]]></description><link>https://www.bitdefender.com/blog/hotforsecurity/attackers-use-old-youtube-feature-in-phishing-attack-to-send-messages-from-legitimate-email/</link><guid isPermaLink=\"false\">643035bbec6c331524f36f08</guid><category><![CDATA[Industry News]]></category><dc:creator>Silviu STAHIE</dc:creator><pubDate>Fri, 07 Apr 2023 15:26:44 GMT</pubDate><media:content url=\"https://blogapp.bitdefender.com/hotforsecurity/content/images/2023/04/youtube-gde828e406_1920.jpg\" medium=\"image\"/><content:encoded><![CDATA[A new phishing campaign using a YouTube feature that helps attackers send emailsfrom a valid email has been making the rounds. Google is already investigatingthe issue, and our telemetry shows the phishing campaign is live.Spam filters can quickly determine if an email is part of a phishing campaign,and one way to do that is by looking at the email address that sent the message.While a spoofed address might trick a user by looking like a real one, theserver won’t make the same mistake.N]]></content:encoded></item><item><title>What Is a Digital ‘Fingerprint’ and Why Do Hackers Want Yours So Badly?</title><description><![CDATA[Websites today collect troves of information about visitors, not only foradvertising, business optimization and user experience, but also for securitypurposes.In addition to cookies, websites use ‘fingerprinting’ to collect informationabout a user’s web browser, hardware, device configuration, time zone, and evenbehavioral patterns, to authorize a legitimate user or de-authorize an impostor.While ‘fingerprints’ are useful in various ways, they’re crucial in detectingand preventing ident]]></description><link>https://www.bitdefender.com/blog/hotforsecurity/what-is-a-digital-fingerprint-and-why-do-hackers-want-yours-so-badly/</link><guid isPermaLink=\"false\">6430121dec6c331524f36ee4</guid><category><![CDATA[Industry News]]></category><category><![CDATA[Digital Privacy]]></category><dc:creator>Filip TRUȚĂ</dc:creator><pubDate>Fri, 07 Apr 2023 14:30:00 GMT</pubDate><media:content url=\"https://blogapp.bitdefender.com/hotforsecurity/content/images/2023/04/george-prentzas-SRFG7iwktDk-unsplash.jpg\" medium=\"image\"/><content:encoded><![CDATA[Websites today collect troves of information about visitors, not only foradvertising, business optimization and user experience, but also for securitypurposes.In addition to cookies, websites use ‘fingerprinting’ to collect informationabout a user’s web browser, hardware, device configuration, time zone, and evenbehavioral patterns, to authorize a legitimate user or de-authorize an impostor.While ‘fingerprints’ are useful in various ways, they’re crucial in detectingand preventing ident]]></content:encoded></item><item><title>Twitter 'Shadow Ban' Flaw Receives Official CVE Number</title><description><![CDATA[In a striking development, cybersecurity researcher Federico Andres Lois hasidentified a critical bug in Twitter's source code that could let threat actorsmanipulate the platform's algorithm and suppress posts from appearing on users'feeds.The flaw, dubbed the \"Shadow Ban\" bug, has been assigned a CVE (CommonVulnerabilities and Exposures) number to highlight its significance, and is nowtracked as CVE-2023-29218[https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2023-29218].Its full des]]></description><link>https://www.bitdefender.com/blog/hotforsecurity/twitter-shadow-ban-flaw-receives-official-cve-number/</link><guid isPermaLink=\"false\">642fceccec6c331524f36ea7</guid><category><![CDATA[Industry News]]></category><dc:creator>Vlad CONSTANTINESCU</dc:creator><pubDate>Fri, 07 Apr 2023 08:08:23 GMT</pubDate><media:content url=\"https://blogapp.bitdefender.com/hotforsecurity/content/images/2023/04/akshar-dave-mkTqZN1NzhY-unsplash--1-.jpg\" medium=\"image\"/><content:encoded><![CDATA[In a striking development, cybersecurity researcher Federico Andres Lois hasidentified a critical bug in Twitter's source code that could let threat actorsmanipulate the platform's algorithm and suppress posts from appearing on users'feeds.The flaw, dubbed the \"Shadow Ban\" bug, has been assigned a CVE (CommonVulnerabilities and Exposures) number to highlight its significance, and is nowtracked as CVE-2023-29218[https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2023-29218].Its full des]]></content:encoded></item></channel>        </rss>";

			/**
			 * Web scraping
			 */
			// Remove content before first item tag
			int itemIndex = content.indexOf("<item>");
			if (itemIndex != -1) {
				content.delete(0, itemIndex);
			}

			String contentStr;
			if (content != null) {
				contentStr = content.toString();
				if (StringUtils.isBlank(contentStr)) {
					shardManager.getTextChannelById(809527650955296848L).sendMessage("Bitdefender contentStr is empty.").queue(); // log channel
					return new ArrayList<>();
				}
			}
			else {
				shardManager.getTextChannelById(809527650955296848L).sendMessage("Bitdefender content is null.").queue(); // log channel
				return new ArrayList<>();
			}

			// Extract pubDate to check if already sent out the latest news
			List<String> pubDates = extractTagContent(contentStr, "pubDate");
			if (pubDates == null || pubDates.isEmpty() || StringUtils.isBlank(pubDates.get(0))) {
				miscService.messageToLog("LoopService.getBitdefender: pubDates from web is blank, cannot be evaluated", false);
				return new ArrayList<>();
			}
			else {
				String pubDate = pubDates.get(0);
				Points points = new Points();
				points.setType("Bitdefender");
				points.setActive("Y");

				String pubDateOld = loopMapper.getPublishedat(points);
				if (StringUtils.isBlank(pubDateOld)) {
					miscService.messageToLog("LoopService.getBitdefender: pubDateOld from Database is blank, cannot be evaluated", false);
					return new ArrayList<>();
				}
				else if (StringUtils.isBlank(pubDate)) {
					miscService.messageToLog("LoopService.getBitdefender: pubDate from Website is blank, cannot be evaluated", false);
					return new ArrayList<>();
				}
				else {
					if (pubDate.trim().equals(pubDateOld.trim())) {
						return new ArrayList<>();
					}
					else {
						points.setPublishedat(pubDate);
						if (!loopMapper.updatePublishedat(points)) {
							miscService.messageToLog("LoopService.getBitdefender: update pubDate --> pubDateOld to Database failed", false);
							return new ArrayList<>();
						}
					}
				}
			}

			// Extract all occurrences of tags from RSS feed
			List<String> titles = extractTagContent(contentStr, "title");
			List<String> descriptions = extractTagContent(contentStr, "description");
			List<String> links = extractTagContent(contentStr, "link");
			List<String> creators = extractTagContent(contentStr, "dc:creator");
			List<String> images = extractTagContentUnmatched(contentStr, "media:content");

			/**
			 * Description replace CDATA
			 *
			 * .replaceAll("<!\\[CDATA\\[(.*?)\\]\\]>", "$1");
			 * .replace("<![CDATA[", "").replace("]]>", "");
			 */
			// replaced enhanced loop with regular loop due to possible duplicates
//			for (String description : descriptions) {
//				if (StringUtils.isNotBlank(description)) {
//					description = description.replaceAll("<!\\[CDATA\\[|\\]\\]>", "");
//					descriptions.set(descriptions.indexOf(description), description);
//				}
//			}
			for (int i = 0; i < descriptions.size(); i++) {
				String description = descriptions.get(i);
				if (StringUtils.isNotBlank(description)) {
					description = description.replaceAll("<!\\[CDATA\\[|\\]\\]>", "");
					descriptions.set(i, description);
				}
			}

			List<MessageEmbed> messageEmbedList = new ArrayList<>(); // list to allow future scalability

			for (int i = 0; i < titles.size(); i++) {
				EmbedBuilder embedBuilder = new EmbedBuilder();

				if (i < links.size()) {
					embedBuilder.setTitle(titles.get(i), links.get(i));
				}
				else {
					embedBuilder.setTitle(titles.get(i));
				}

				if (i < descriptions.size()) {
					embedBuilder.setDescription(descriptions.get(i));
				}

				if (i < creators.size()) {
					embedBuilder.setAuthor("Bitdefender Cybersecurity Blogs");
//					embedBuilder.setAuthor(creators.get(i));
				}

				if (i < pubDates.size()) {
					embedBuilder.setFooter(pubDates.get(i));
				}

				if (i < images.size()) {
					embedBuilder.setThumbnail(images.get(i));
				}

				messageEmbedList.add(embedBuilder.build());
				break; // latest only
			}


			/**
			 * Return result
			 */
			return messageEmbedList;
		}
		else {
			shardManager.getTextChannelById(809527650955296848L).sendMessage(String.format("Bitdefender response code: ", responseCode)).queue(); // log channel
			return new ArrayList<>();
		}
	}

	//	private static String extractTagContent(String xml, String tagName) {
	//		int startTagIndex = xml.indexOf("<" + tagName + ">");
	//		int endTagIndex = xml.indexOf("</" + tagName + ">", startTagIndex);
	//		if (startTagIndex != -1 && endTagIndex != -1) {
	//			return xml.substring(startTagIndex + tagName.length() + 2, endTagIndex).trim();
	//		} else {
	//			return "";
	//		}
//	}

	/**
	 * Extract content from <tag>content</tag>
	 * @param xml
	 * @param tagName
	 * @return
	 */
	private static List<String> extractTagContent(String xml, String tagName) {
		List<String> tagContents = new ArrayList<>();
		int startTagIndex = xml.indexOf("<" + tagName + ">");
		while (startTagIndex != -1) {
			int endTagIndex = xml.indexOf("</" + tagName + ">", startTagIndex);
			if (endTagIndex != -1) {
				tagContents.add(xml.substring(startTagIndex + tagName.length() + 2, endTagIndex).trim());
				startTagIndex = xml.indexOf("<" + tagName + ">", endTagIndex);
			} else {
				break;
			}
		}
		return tagContents;
	}

	/**
	 * Extract content from <tag url="content"/>
	 * @param xml
	 * @param tagName
	 * @return
	 */
	private static List<String> extractTagContentUnmatched(String xml, String tagName) {
		List<String> tagContents = new ArrayList<>();
		int startTagIndex = xml.indexOf("<" + tagName);
		while (startTagIndex != -1) {
			int tagEndIndex = xml.indexOf(">", startTagIndex);
			if (tagEndIndex != -1) {
				String tagContent = xml.substring(startTagIndex, tagEndIndex + 1).trim();
				if (tagName.equals("media:content")) {
					int urlIndex = tagContent.indexOf("url=");
					if (urlIndex != -1) {
						int urlStartIndex = urlIndex + 5;
						int urlEndIndex = tagContent.indexOf("\"", urlStartIndex);
						if (urlEndIndex != -1) {
							tagContents.add(tagContent.substring(urlStartIndex, urlEndIndex));
						}
					}
				} else {
					tagContents.add(tagContent);
				}
				startTagIndex = xml.indexOf("<" + tagName, tagEndIndex);
			} else {
				break;
			}
		}
		return tagContents;
	}
}
