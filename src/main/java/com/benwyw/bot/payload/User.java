package com.benwyw.bot.payload;

import lombok.Data;

@Data
public class User {

	private String userId;
	private String userTag;
	private String userName;
	
	private String userBalance;
	private String colorPref;
	private String userWin;
	private String mcName;
	private String riotLolTwName;
	private String riotLolNaName;
	private String ownedPlaylistCount;
	private String linkedPlaylistCount;
}
