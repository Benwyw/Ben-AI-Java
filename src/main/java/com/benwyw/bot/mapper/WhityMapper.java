package com.benwyw.bot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.benwyw.bot.data.WhityWeight;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WhityMapper extends BaseMapper<WhityWeight> {
	List<WhityWeight> getWhityWeight(IPage<WhityWeight> page);
	long getWhityWeightCount();
	WhityWeight selectLatest();

	List<WhityWeight> select();
	int insert(WhityWeight whityWeight);
	int update(WhityWeight whityWeight);
	int delete(@Param("recordId") int recordId);
}
