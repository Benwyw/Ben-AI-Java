package com.benwyw.bot.controller.web;

import com.benwyw.bot.service.SwaggerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@RestController
@RequestMapping("swagger")
public class SwaggerController {

//	@Autowired
//	ShardManager shardManager;
	
//	@Autowired
//	CommandListener commandListener;
	
	@Autowired
	SwaggerService swaggerService;

	/**
	 * Generate Excel with Swagger JSON passed in Frontend
	 * Deprecated
	 * @param jsonString Swagger JSON text
	 * @return ResponseEntity<byte[]>
	 * @throws IOException IOException
	 */
	@Deprecated
	@PostMapping("/jsonStringToExcel")
	@Profile("local")
	public ResponseEntity<byte[]> jsonStringToExcel(@RequestBody String jsonString) throws IOException {
		return swaggerService.jsonStringToExcel(jsonString);
	}

	/**
	 * Generate Excel with Swagger JSON passed in Frontend
	 * @param jsonString Swagger JSON text
	 * @return ResponseEntity<StreamingResponseBody>
	 * @throws IOException IOException
	 */
	@PostMapping("/generateExcelFromSwaggerJson")
	public ResponseEntity<StreamingResponseBody> generateExcelFromSwaggerJson(@RequestBody String jsonString) throws IOException {
		return swaggerService.generateExcelFromSwaggerJson(jsonString);
	}

	/**
	 * Generate Excel to output directory with Swagger JSON passed in API call in Local machine
	 * @param jsonString Swagger JSON text
	 * @return ResponseEntity<Resource>
	 * @throws IOException IOException
	 */
	@PostMapping("/generateExcelFromSwaggerJsonLocal")
	@Profile("local")
	public ResponseEntity<Resource> generateExcelFromSwaggerJsonLocal(@RequestBody String jsonString) throws IOException {
		Resource resource = swaggerService.generateExcelFromSwaggerJsonLocal(jsonString);

		// Create a download link for the file and return it as the response body
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		String downloadLink = "/api/download/" + resource.getFilename();
		return ResponseEntity.ok()
				.headers(headers)
				.body(new ByteArrayResource(downloadLink.getBytes()));
	}

	/**
	 * Download file intend to work with generateExcelFromSwaggerJsonLocal in Local machine
	 * @param filename in output directory
	 * @return file download
	 */
	@GetMapping("/download/{filename:.+}")
	@Profile("local")
	public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
		// Create a FileSystemResource for the file and return it as the response body
		Path file = Paths.get("output").resolve(filename);
		Resource resource = new FileSystemResource(file.toFile());

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());

		return ResponseEntity.ok()
				.headers(headers)
				.body(resource);
	}

	/**
	 * Generate Excel to output directory with Swagger JSON swagger.json file in Local machine
	 * @throws IOException IOException
	 */
	@GetMapping("/generateExcelFromSwaggerJson")
	@Profile("local")
	public void generateExcelFromSwaggerJson() throws IOException {
		swaggerService.generateExcelFromSwaggerJson();
	}

	/**
	 * Generate Excel to output directory with Swagger JSON URL in Local machine
	 * @throws IOException IOException
	 */
	@GetMapping("/generateExcelFromSwaggerJsonUrl")
	@Profile("local")
	public void generateExcelFromSwaggerJsonUrl() throws IOException {
		swaggerService.generateExcelFromSwaggerJsonUrl();
	}

	/**
	 * Trigger in background with real-time progress in server log only
	 * Experiment with long-running task with Thread
	 * @return Triggered message
	 */
	@GetMapping("/getThread")
	@Profile("local")
	@ResponseStatus(HttpStatus.OK)
	public ResponseEntity<String> getThread() {
		return swaggerService.getThread();
	}

	/**
	 * Trigger with real-time progress to client directly
	 * Experiment with long-running task with Thread
	 * @return Real-time progress
	 */
	@GetMapping(value = "/getProgress", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	@Profile("local")
	public SseEmitter getProgress() {
		return swaggerService.getProgress();
	}

}
