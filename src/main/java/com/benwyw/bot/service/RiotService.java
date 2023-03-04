package com.benwyw.bot.service;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.benwyw.bot.data.RiotUser;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class RiotService {
	
//	@Autowired
//	private RiotUserMapper riotUserMapper;

	public boolean isValidUserName(RiotUser riotUser) {
		String region = "";
		switch (riotUser.getRegion()) {
		case "TW":
			region = "tw2";
			break;
		case "NA":
			region = "na1";
			break;
		default:
			log.error("riotUser.getRegion() is empty!");
			break;
		}
		
		if (StringUtils.isBlank(region))
			return false;
		
		final String url = String.format("https://%s.api.riotgames.com/lol/summoner/v4/summoners/by-name/%s", region, riotUser.getUserName());
		String token = Dotenv.configure().load().get("RIOT_API_KEY");

		HttpHeaders headers = new HttpHeaders();
		headers.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.0.0 Safari/537.36 Edg/109.0.1518.70");
		headers.add("Accept-Language", "zh-TW,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6");
		headers.add("Accept-Charset", "application/x-www-form-urlencoded; charset=UTF-8");
		headers.add("X-Riot-Token", token);
//		
//		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
//		headers.set("X-COM-PERSIST", "NO");
//		headers.set("X-COM-LOCATION", "USA");

		HttpEntity<String> entity = new HttpEntity<String>(headers);
		RestTemplate restTemplate = new RestTemplate();
		
		try {
			ResponseEntity<JSONObject> responseEntity = restTemplate.exchange(url, HttpMethod.GET, entity, JSONObject.class);
			if (responseEntity.getStatusCode() != null && String.valueOf(responseEntity.getStatusCode()).contains("200"))
				return true;
			else {
				log.info(String.format("Unknown error occured: %s", String.valueOf(responseEntity.getStatusCode())));
				return false;
			}
		} catch(Exception e) {
			if (String.valueOf(e).contains("404"))
				return false;
		}
		
		return false;
	}
	
//	public int getCountExistingLinkedUserName(RiotUser riotUser) {
//		return riotUserMapper.getCountExistingLinkedUserName(riotUser);
//	}
}
