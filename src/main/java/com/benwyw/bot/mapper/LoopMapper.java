package com.benwyw.bot.mapper;

import com.benwyw.bot.data.Points;
import com.benwyw.bot.data.RiotUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LoopMapper {

	String getPublishedat(Points Points);
	boolean updatePublishedat(Points points);

}
