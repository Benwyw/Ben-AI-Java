package com.benwyw.bot.mapper;

import com.benwyw.bot.data.security.RefreshToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RefreshTokenMapper {
    int insertToken(RefreshToken token);
    int isValid(@Param("jti") String jti,
                @Param("tokenHash") String tokenHash);
    int revokeByJti(@Param("jti") String jti);
    int revokeAllForUser(@Param("userId") Long userId);
    int purgeExpiredOrRevoked();
    int countExpiredOrRevoked();
    int deleteTokensByUserId(@Param("userId") long userId);

}
