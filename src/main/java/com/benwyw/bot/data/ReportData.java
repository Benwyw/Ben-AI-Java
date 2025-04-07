package com.benwyw.bot.data;

import lombok.Data;

@Data
public class ReportData {

	private String name;
	private int age;
	private String country;

	public ReportData(String name, int age, String country) {
		this.name = name;
		this.age = age;
		this.country = country;
	}

}
