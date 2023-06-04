package com.benwyw.bot.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Service
public class SwaggerService {

//	@Autowired
//	@SuppressWarnings("unused")
//	private ShardManager shardManager;

	/**
	 * Generate file name by system time
	 * @return file name with timestamp
	 */
	public String getFileName() {
		LocalDateTime now = LocalDateTime.now();
		String formattedDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
		return String.format("swagger-%s.xlsx", formattedDate);
	}

	/**
	 * Utility - Handle workbook to output directory
	 * @param workbook generated XSSFWorkbook
	 * @return path to output file
	 * @throws IOException IOException
	 */
	@NotNull
	private Path getOutputFile(XSSFWorkbook workbook) throws IOException {
		// Write the workbook to a file in the output directory
		Path outputDir = Paths.get("output");
		if (!Files.exists(outputDir)) {
			Files.createDirectories(outputDir);
		}
		String fileName = getFileName();
		Path outputFile = outputDir.resolve(fileName);
		workbook.write(Files.newOutputStream(outputFile));
		workbook.close();
		return outputFile;
	}

	/**
	 * Utility - Generate XSSFWorkbook with JSON data
	 * @param data converted JSON Map
	 * @return XSSFWorkbook
	 */
	private XSSFWorkbook generateWorkbook(Map<String, Object> data) {
		// Create a new workbook and select the active worksheet
		XSSFWorkbook workbook = new XSSFWorkbook();
		Sheet sheet = workbook.createSheet();

		// Write headers for the summary, API method, and endpoint columns
		Row headerRow = sheet.createRow(0);
		headerRow.createCell(0).setCellValue("Function services");
		headerRow.createCell(1).setCellValue("Program ID");
		headerRow.createCell(2).setCellValue("HTTP Method");
		headerRow.createCell(3).setCellValue("API Path");

		// Loop through each endpoint and method in the Swagger JSON file and write the summary, API method, and endpoint to the worksheet
		int row = 1;
		Map<String, Map<String, Object>> paths = (Map<String, Map<String, Object>>) data.get("paths");
		for (String endpoint : paths.keySet()) {
			Map<String, Object> methods = paths.get(endpoint);
			for (String method : methods.keySet()) {
				Map<String, Object> attributes = (Map<String, Object>) methods.get(method);
				String summary = (String) attributes.getOrDefault("summary", "");
				String description = (String) attributes.getOrDefault("description", "");
				Row dataRow = sheet.createRow(row++);
				dataRow.createCell(0).setCellValue(summary);
				dataRow.createCell(1).setCellValue(description);
				dataRow.createCell(2).setCellValue(method);
				dataRow.createCell(3).setCellValue(endpoint);
			}
		}

		return workbook;
	}

	/**
	 * Convertor - JSON String to Excel, old approach
	 * @param jsonString Swagger JSON text
	 * @return bytes[] contains xlsx
	 * @throws IOException IOException
	 */
	public ResponseEntity<byte[]> jsonStringToExcel(String jsonString) throws IOException {
		// Read the Swagger JSON file into a Map
		ObjectMapper objectMapper = new ObjectMapper();
//		Map<String, Object> data = objectMapper.readValue(new File("swagger.json"), Map.class);
		String convertedJsonString = (String) objectMapper.readValue(jsonString, Map.class).get("jsonString");
		Map<String, Object> data = objectMapper.readValue(convertedJsonString, new TypeReference<>() {});

		XSSFWorkbook workbook = generateWorkbook(data);

//		// Create the output directory if it doesn't exist
//		Path outputDir = Paths.get("output");
//		if (!Files.exists(outputDir)) {
//			Files.createDirectories(outputDir);
//		}
//
//		// Append the current system date and time to the filename
//		LocalDateTime now = LocalDateTime.now();
//		String formattedDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
//		String filename = String.format("swagger-%s.xlsx", formattedDate);
//
//		// Save the workbook to the output subdirectory with the annotated filename
//		workbook.write(Files.newOutputStream(outputDir.resolve(filename)));
//		workbook.close();

		// Set the headers for the response
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		headers.setContentDisposition(ContentDisposition.attachment().filename("data.xlsx").build());

		// Write the contents of the workbook to the response output stream
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		workbook.write(outputStream);
		byte[] content = outputStream.toByteArray();
		outputStream.close();
		workbook.close();

		return new ResponseEntity<>(content, headers, HttpStatus.OK);
	}

	/**
	 * Convertor - JSON String to Excel, new approach
	 * When downloading large files from a server, using a StreamingResponseBody can be more efficient than returning a byte[] because it allows the server to stream the response data to the client in small chunks, rather than buffering the entire response in memory before sending it to the client.
	 *
	 * This can help reduce the memory usage and network bandwidth requirements for the server, as well as improve the perceived performance for the client by allowing the user to begin downloading the file immediately, rather than waiting for the entire response to be buffered.
	 *
	 * In contrast, returning a byte[] requires the server to buffer the entire response in memory before sending it to the client, which can be inefficient and may not scale well for large files or high volumes of concurrent requests.
	 *
	 * Overall, using a StreamingResponseBody can help improve the efficiency and performance of file downloads in web applications.
	 * @param jsonString Swagger JSON text
	 * @return StreamingResponseBody contains xlsx
	 * @throws IOException IOException
	 */
	public ResponseEntity<StreamingResponseBody> generateExcelFromSwaggerJson(String jsonString) throws IOException {
		// Read the Swagger JSON file into a Map
		ObjectMapper objectMapper = new ObjectMapper();
		String convertedJsonString = objectMapper.readValue(jsonString, new TypeReference<Map<String, String>>() {}).get("jsonString");
		Map<String, Object> data = objectMapper.readValue(convertedJsonString, new TypeReference<>() {});

		// Set the headers for the response
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		headers.setContentDisposition(ContentDisposition.attachment().filename("data.xlsx").build());

		// Create a StreamingResponseBody to stream the contents of the workbook to the response output stream
		StreamingResponseBody responseBody = outputStream -> {
			try (XSSFWorkbook workbook = generateWorkbook(data)) {
				workbook.write(outputStream);
			} catch (IOException e) {
				log.error(e.toString());
			}
		};

		return new ResponseEntity<>(responseBody, headers, HttpStatus.OK);
	}

	/**
	 * Convertor - Read JSON text from API RequestBody for local machine
	 * @param jsonString Swagger JSON text
	 * @return xlsx in output directory
	 * @throws IOException IOException
	 */
	public Resource generateExcelFromSwaggerJsonLocal(String jsonString) throws IOException {
		// Read the Swagger JSON file into a Map
		ObjectMapper objectMapper = new ObjectMapper();
		Map<String, Object> data = objectMapper.readValue(jsonString, new TypeReference<>() {});

		XSSFWorkbook workbook = generateWorkbook(data);

		Path outputFile = getOutputFile(workbook);

		// Create a FileSystemResource for the output file and return it as the response body
		return new FileSystemResource(outputFile.toFile());
	}


	/**
	 * Convertor - Read JSON text from swagger.json and output the Excel to output directory for Local machine
	 * @throws IOException IOException
	 */
	public void generateExcelFromSwaggerJson() throws IOException {
		// Read the Swagger JSON file into a Map
		ObjectMapper objectMapper = new ObjectMapper();
		Map<String, Object> data = objectMapper.readValue(new File("swagger.json"), new TypeReference<>() {});

		XSSFWorkbook workbook = generateWorkbook(data);

		// Write the workbook to a file in the output directory
		getOutputFile(workbook);
	}

	/**
	 * Convertor - Read JSON text from submitted JSON file and return the Excel for Discord command
	 * @throws IOException IOException
	 */
	public File generateExcelFromSwaggerJson(File file) throws IOException {
		// Read the Swagger JSON file into a Map
		ObjectMapper objectMapper = new ObjectMapper();
		Map<String, Object> data = objectMapper.readValue(file, new TypeReference<>() {});

		XSSFWorkbook workbook = generateWorkbook(data);

		// Create a temp file
		File tempFile = File.createTempFile("swagger-excel", ".xlsx");

		// Write the workbook to the temp file
		workbook.write(new FileOutputStream(tempFile));

		log.info("tempFile.getName(): "+tempFile.getName());

		// Return the temp file
		return tempFile;
	}
}
