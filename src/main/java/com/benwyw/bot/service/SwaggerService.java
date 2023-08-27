package com.benwyw.bot.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
		headerRow.createCell(0).setCellValue("Module");
		headerRow.createCell(1).setCellValue("Function services");
		headerRow.createCell(2).setCellValue("Program ID");
		headerRow.createCell(3).setCellValue("HTTP Method");
		headerRow.createCell(4).setCellValue("API Path");

		// Set module
		String module = "";
		Map<String, Map<String, Object>> infos = (Map<String, Map<String, Object>>) data.get("info");
		for (String infoType : infos.keySet()) {
			if ("title".equals(infoType)) {
				module = String.valueOf(infos.get(infoType));
				break;
			}
		}

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
				dataRow.createCell(0).setCellValue(module);
				dataRow.createCell(1).setCellValue(summary);
				dataRow.createCell(2).setCellValue(description);
				dataRow.createCell(3).setCellValue(method.toUpperCase());
				dataRow.createCell(4).setCellValue(endpoint);
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
	 * Convertor - Read JSON text from URL and output the Excel to output directory for Local machine
	 * @throws IOException IOException
	 */
	public void generateExcelFromSwaggerJsonUrl() throws IOException {
		// Fetch the Swagger JSON content from a URL
		String swaggerJsonUrl = "https://raw.githubusercontent.com/OAI/OpenAPI-Specification/main/examples/v2.0/json/petstore-simple.json";
		URL url = new URL(swaggerJsonUrl);
		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		connection.setRequestProperty("Accept", "application/json");
		InputStream inputStream = connection.getInputStream();
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
		StringBuilder responseBuilder = new StringBuilder();
		String line;
		while ((line = bufferedReader.readLine()) != null) {
			responseBuilder.append(line);
		}
		String swaggerJsonContent = responseBuilder.toString();

		// Parse the Swagger JSON content into a Map
		ObjectMapper objectMapper = new ObjectMapper();
		Map<String, Object> data = objectMapper.readValue(swaggerJsonContent, new TypeReference<>() {});

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

	/**
	 * Thread sleep based on ms passed in
	 * @param remainingTimeMillis ms
	 */
	private static void sleep(long remainingTimeMillis) {
		while (remainingTimeMillis > 0) {
			log.info("Sleeping for " + TimeUnit.MILLISECONDS.toSeconds(remainingTimeMillis) + " seconds remaining");

			try {
				Thread.sleep(Math.min(remainingTimeMillis, 1000)); // Sleep for up to 1 second at a time
			} catch (Exception e) { // InterruptedException
				e.printStackTrace();
			}

			remainingTimeMillis -= 1000; // Subtract 1 second from remaining time
		}
	}

	/**
	 * Trigger in background with real-time progress in server log only
	 * Experimental thread sleep
	 * @return Status
	 */
	public ResponseEntity<String> getThread() {
		try {
			new Thread(() -> {
				log.info("Pause execution thread started");
				try {
					sleep(TimeUnit.SECONDS.toMillis(10));
				} catch (Exception e) {
					log.error("Sleep interrupted", e);
				}
			}).start();
			return ResponseEntity.ok("Long running task started in the background.");
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(String.format("Error starting long running task: %s", e.getMessage()));
		}
	}

//	public SseEmitter getProgress() {
//		SseEmitter emitter = new SseEmitter();
//
//		new Thread(() -> {
//			int total = 10;
//			int progress = 0;
//
//			while (progress < total) {
//				try {
//					sleep(TimeUnit.SECONDS.toMillis(10)); // Sleep for 1 second
//					progress++;
//					double percentage = (double) progress / total * 100;
//					emitter.send(SseEmitter.event().data(String.format("%.2f%%", percentage))); // Send progress update
//				} catch (Exception e) {
//					emitter.completeWithError(e); // Complete the emitter if an error occurs
//					return;
//				}
//			}
//
//			emitter.complete(); // Complete the emitter when done
//		}).start();
//
//		return emitter;
//	}

	/**
	 * Thread sleep based on ms passed in
	 * @param remainingTimeMillis ms
	 * @param emitter SseEmitter to send progress updates to the client
	 */
	private static void sleep(long remainingTimeMillis, SseEmitter emitter) {
		while (remainingTimeMillis > 0) {
			float percentageComplete = 100 * (1 - ((float)remainingTimeMillis / TimeUnit.SECONDS.toMillis(10)));
			try {
				emitter.send(SseEmitter.event().data(percentageComplete));
			} catch (IOException e) {
				e.printStackTrace();
			}

			try {
				Thread.sleep(Math.min(remainingTimeMillis, 1000)); // Sleep for up to 1 second at a time
			} catch (Exception e) { // InterruptedException
				e.printStackTrace();
			}

			remainingTimeMillis -= 1000; // Subtract 1 second from remaining time
		}

		try {
			emitter.send(SseEmitter.event().data(100f)); // Report completion
			emitter.complete();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Trigger with real-time progress to client directly
	 * Experimental thread sleep
	 * @return Status
	 */
	public SseEmitter getProgress() {
		SseEmitter emitter = new SseEmitter();
		try {
			new Thread(() -> {
				log.info("Pause execution thread started");
				try {
					sleep(TimeUnit.SECONDS.toMillis(10), emitter);
				} catch (Exception e) {
					log.error("Sleep interrupted", e);
				}
			}).start();
		} catch (Exception e) {
			emitter.completeWithError(e);
		}
		return emitter;
	}

	@Async
	protected CompletableFuture<String> asyncMethod() {
		/* Can be achieved with the same result, but only one static return at the end
		return CompletableFuture.supplyAsync(() -> {
		   return "Completed";
		});
		 */

//		CompletableFuture<String> future = new CompletableFuture<>();
//
//		// Async logic here
//		for (int i = 0; i < 10; i++) {
//			log.info(String.format("%s - %s", Thread.currentThread().getName(), i));
//		}
//
//		// Complete the future when the async task is finished
//		future.complete(String.format("Async method completed successfully for %s", Thread.currentThread().getName()));
//		return future;


		return CompletableFuture.supplyAsync(() -> {

			// Async logic here
			for (int i = 0; i < 10; i++) {
				log.info(String.format("%s - %s", Thread.currentThread().getName(), i));
			}

			// Complete the future when the async task is finished
			return String.format("Async method completed successfully for %s", Thread.currentThread().getName());
		});
	}

	@Async
	protected void anotherAsyncMethod(String str) {
		log.info(str);
	}

	@Async
	public void asyncMultithreadMethod() {
		log.info("Started future1");
		CompletableFuture<String> future1 = asyncMethod();
		log.info("Started future2");
		CompletableFuture<String> future2 = asyncMethod();
		log.info("Started future3");
		CompletableFuture<String> future3 = asyncMethod();
		log.info("Started future4");
		CompletableFuture<String> future4 = asyncMethod();
		log.info("Started future5");
		CompletableFuture<String> future5 = asyncMethod();

		CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(future1, future2, future3, future4, future5);
		combinedFuture.thenRun(() -> {
			future1.thenAcceptAsync(this::anotherAsyncMethod); // result -> anotherAsyncMethod(result)
			future2.thenAcceptAsync(this::anotherAsyncMethod);
			future3.thenAcceptAsync(this::anotherAsyncMethod);
			future4.thenAcceptAsync(this::anotherAsyncMethod);
			future5.thenAcceptAsync(this::anotherAsyncMethod);
		});
	}

	/**
	 * Print list of any object to line by line with CSVWriter
	 * @param list List<T>
	 * @param writer CSVWriter
	 * @return CSVWriter
	 * @throws NoSuchMethodException NoSuchMethodException
	 * @throws InvocationTargetException InvocationTargetException
	 * @throws IllegalAccessException IllegalAccessException
	 */
	private <T> CSVWriter printList(List<T> list, CSVWriter writer) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		if (list == null || list.isEmpty() || ObjectUtils.isEmpty(list.get(0))) {
			return writer;
		}

		// Get the list of property names from the first object in the list
		T firstObject = list.get(0);
		Field[] fields = firstObject.getClass().getDeclaredFields();
		List<String> propertyNames = new ArrayList<>();
		for (Field field : fields) {
			propertyNames.add(field.getName());
		}

		// First row
//		writer.writeNext(propertyNames.toArray(new String[0]));

		// Write the values to the CSV file
		for (T object : list) {
			List<String> values = new ArrayList<>();

			for(String propertyName : propertyNames) {
				// Get the value of the property using reflection
				Method method = object.getClass().getMethod("get" + StringUtils.capitalize(propertyName));
				Object value = method.invoke(object);

				// Convert the value to a string and add it to the list of values
				if (value == null || value == "null") {
					values.add("");
				}
				else if (value.toString().contains(",")) {
					values.add(String.format("\"%s\"", value));
				}
				else {
					values.add(String.format("=\"%s\"", value));
				}
			}

			writer.writeNext(values.toArray(new String[0]));
		}

		return writer;
	}

	/**
	 * Create CSV File
	 * @return File
	 * @throws Exception Exception
	 */
	public File createCSVFile() throws Exception {
		// Temp variables for passed in object
		List<String> upHeader = Arrays.asList("uH1", "uH2", "uH3");
		List<String> upBody = Arrays.asList("uB1", "uB2", "uB3");
		List<String> downHeader = Arrays.asList("dH1", "dH2", "dH3");
		List<String> downBody = Arrays.asList("dB1", "dB2", "dB3");
		String reportId = "Test";
		String fileType = ".csv";
		String dedicatedPath = "D:/";

		// Construct unique file name
		LocalDateTime localDateTime = LocalDateTime.now();
		String date = localDateTime.format(DateTimeFormatter.BASIC_ISO_DATE);
		String fileName = String.format("%s_%s_%s", reportId, date, fileType);
		String fullPath = dedicatedPath + fileName;

		// Additional pre-header operations adds here
		// ...

		// Create CSV File
		File file = new File(fullPath);
		File parentFile = file.getParentFile();
		if (parentFile != null && !parentFile.exists()) {
			parentFile.mkdirs();
		}
		file.createNewFile();

		// Initialize writers
		FileWriter fileWriter = new FileWriter(file, false);
		CSVWriter csvWriter = new CSVWriter(fileWriter);

		// Write to CSV
		try {
			// Header
			for (int count = 0; count < upHeader.size(); count++) {
				csvWriter.writeNext(new String[] {upHeader.get(count), upBody.get(count)});
			}

			// Body
			csvWriter.writeNext(downHeader.toArray(String[]::new));
			printList(downBody, csvWriter);
		} finally {
			csvWriter.flush();
			csvWriter.close();
		}

		return file;
	}
}
