package com.benwyw.bot.data;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("feature")
public class Feature {

	private int featureId;
	private String featureName;
	private String featureDescription;
	private Integer parentId;

}
