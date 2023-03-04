package com.benwyw.bot.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.benwyw.bot.data.User;

@Mapper
public interface UserMapper {
	public User getUserInfo(@Param("userId") String userId);
}
