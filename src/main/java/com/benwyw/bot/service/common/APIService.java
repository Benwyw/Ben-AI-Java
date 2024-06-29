package com.benwyw.bot.service.common;

import com.benwyw.bot.service.LogService;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

@Slf4j
@Service
public class APIService {

	@Autowired
	private LogService logService;

	/**
	 * Required param
	 * url
	 *
	 * Optional param
	 * HttpHeaders customHeader - customHeader.add("X-Riot-Token", token);
	 * @param url String
	 * @param httpMethod HttpMethod
	 * @param customHeaders HttpHeaders
	 * @return ResponseEntity
	 */
	public ResponseEntity<JSONObject> exchange(String url, HttpMethod httpMethod, HttpHeaders customHeaders) {
		HttpHeaders headers = new HttpHeaders();
		headers.addAll(customHeaders);
		headers.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.0.0 Safari/537.36 Edg/109.0.1518.70");
		headers.add("Accept-Language", "zh-TW,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6");
		headers.add("Accept-Charset", "application/x-www-form-urlencoded; charset=UTF-8");

		HttpEntity<String> httpEntity = new HttpEntity<>(headers);

		RestTemplate restTemplate = new RestTemplate();
		return restTemplate.exchange(url, httpMethod, httpEntity, JSONObject.class);
	}

	/**
	 * Check if URL is accessible
	 * @param serverUrl String
	 * @return boolean
	 */
	public boolean isAccessible(String serverUrl) {
		try {
			URL url = new URL(serverUrl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("HEAD");

			int responseCode = connection.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
//				logService.messageToLog(String.format("%s is accessible!", serverUrl), true);
				return true;
			} else {
				logService.messageToLog(String.format("%s is not accessible (HTTP response code: %s)", serverUrl, responseCode), false);
				return false;
			}
		} catch (IOException e) {
			logService.messageToLog(String.format("Error while checking %s status: %s", serverUrl, e.getMessage()), false);
			return false;
		}
	}

}
