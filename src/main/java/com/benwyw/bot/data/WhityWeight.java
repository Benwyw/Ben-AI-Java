package com.benwyw.bot.data;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("WHITY_WEIGHT")
public class WhityWeight {

	private Integer recordId;

	@DateTimeFormat(pattern = "yyyyMMdd")
	@JsonFormat(pattern = "yyyy-MM-dd")
	private LocalDate recordDate;

	private BigDecimal kg;

	private String remarks;

}
