package com.benwyw.bot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.benwyw.bot.data.Feature;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface MiscMapper extends BaseMapper<Feature> {
	List<Feature> getFeatures(IPage<Feature> page);
	long getFeaturesCount();
}
