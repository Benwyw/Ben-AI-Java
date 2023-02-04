package com.benwyw.bot.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.benwyw.bot.data.RiotUser;

@Mapper
public interface RiotUserMapper {
	public int insertUserInfo(RiotUser riotUser);
	public List<RiotUser> getUserInfo(@Param("userId") String userId);
	public int updateUserInfo(RiotUser riotUser);
	public int deleteUserInfo(RiotUser riotUser);
	public List<RiotUser> getExistingLinkedUser(RiotUser riotUser);
	
	public boolean isExistingPoints(@Param("region") String region, @Param("userName") String userName);
	public int insertActivePoints(@Param("region") String region, @Param("userName") String userName);
	public int updateToActivePoints(@Param("region") String region, @Param("userName") String userName);
	public int updateExistingActivePoints(@Param("region") String region, @Param("userName") String userName, @Param("existingUserName") String existingUserName);
	public int softDeletePoints(@Param("region") String region, @Param("userName") String userName);
	public int insertInactivePoints(@Param("region") String region, @Param("userName") String userName);
}
