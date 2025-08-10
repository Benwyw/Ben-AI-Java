package com.benwyw.bot.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.benwyw.bot.data.User;

@Mapper
public interface UserMapper {
	User getUserInfo(@Param("userId") String userId);

    // Security
    com.benwyw.bot.data.security.User findByUsername(@Param("username") String username);
    int insertUser(com.benwyw.bot.data.security.User user);
    int updateLastLogin(@Param("userId") Long userId);
    int deleteUserByUsername(@Param("username") String username);

}
