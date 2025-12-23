
package com.benwyw.bot.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.benwyw.bot.data.User;
import java.util.List;

public interface UserMapper {
	User getUserInfo(@Param("userId") String userId);

    // Security
    com.benwyw.bot.data.security.User findByUsername(@Param("username") String username);
    int insertUser(com.benwyw.bot.data.security.User user);
    int updateLastLogin(@Param("userId") Long userId);

    // User list with pagination
    List<com.benwyw.bot.data.security.User> listUsers(@Param("offset") int offset, @Param("limit") int limit);
    int countUsers();
    int deleteUserByUsername(@Param("username") String username);

}
