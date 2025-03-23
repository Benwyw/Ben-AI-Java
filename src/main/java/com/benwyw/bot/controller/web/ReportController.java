package com.benwyw.bot.controller.web;

import com.benwyw.bot.service.ReportService;
import com.crystaldecisions.sdk.occa.report.lib.ReportSDKException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.util.Arrays;

@Slf4j
@RestController
@RequestMapping("report")
public class ReportController {

	private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

	@Autowired
	private ReportService reportService;

	@GetMapping("/generate-test-report-1")
	public ResponseEntity<StreamingResponseBody> generateTestReport1(
			//@RequestParam(defaultValue = "C:\\Users\\User\\Downloads\\GeneralRpt.rpt") String reportPath,
												 @RequestParam(defaultValue = "1") String dueDays,
												 @RequestParam(defaultValue = "2") String customerNum,
												 @RequestParam(defaultValue = "3") String rmName,
												 @RequestParam(defaultValue = "4") String groupNum,
												 @RequestParam(defaultValue = "5") String customerName,
												 @RequestParam(defaultValue = "6") String sccStatus,
												 @RequestParam(defaultValue = "7") String footText) throws IOException, ReportSDKException {

		byte[] reportData = reportService.generateTestReport1(dueDays, customerNum, rmName, groupNum, customerName, sccStatus, footText);
		if (reportData == null || reportData.length == 0) {
			logger.error("Generated report data is empty.");
			throw new RuntimeException("Report generation failed: no data in report.");
		}

		// Set the response headers
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_PDF);
		headers.setContentDisposition(ContentDisposition.attachment().filename("report1.pdf").build());

		// Create a StreamingResponseBody to stream the PDF report
		StreamingResponseBody responseBody = outputStream -> {
			try {
				outputStream.write(reportData);
				outputStream.flush();
			} catch (IOException e) {
				log.error(e.toString());
			}
        };

		logger.info("Report data header: {}", Arrays.toString(Arrays.copyOf(reportData, 10)));
		return new ResponseEntity<>(responseBody, headers, HttpStatus.OK);
	}

	@GetMapping("/generate-test-report-2")
	public ResponseEntity<StreamingResponseBody> generateTestReport2(
			//@RequestParam(defaultValue = "C:\\Users\\User\\Downloads\\GeneralRpt.rpt") String reportPath,
			@RequestParam(defaultValue = "Tester") String userName) throws IOException, ReportSDKException {
		// Set the response headers
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		headers.setContentDisposition(ContentDisposition.attachment().filename("report2.pdf").build());

		// Create a StreamingResponseBody to stream the PDF report
		StreamingResponseBody responseBody = outputStream -> {
			try {
				byte[] reportData = reportService.generateTestReport2(userName);
				outputStream.write(reportData);
				outputStream.flush();
			} catch (IOException e) {
				log.error(e.toString());
				throw new RuntimeException(e);
			} catch (ReportSDKException e) {
				log.error(e.toString());
				throw new RuntimeException(e);
            }
        };

		return new ResponseEntity<>(responseBody, headers, HttpStatus.OK);
	}

	@GetMapping("/generate-test-report-1-local")
	@Profile("local")
	public ResponseEntity<byte[]> generateTestReport1Local(
			//@RequestParam(defaultValue = "C:\\Users\\User\\Downloads\\GeneralRpt.rpt") String reportPath,
			@RequestParam(defaultValue = "1") String dueDays,
			@RequestParam(defaultValue = "2") String customerNum,
			@RequestParam(defaultValue = "3") String rmName,
			@RequestParam(defaultValue = "4") String groupNum,
			@RequestParam(defaultValue = "5") String customerName,
			@RequestParam(defaultValue = "6") String sccStatus,
			@RequestParam(defaultValue = "7") String footText) throws IOException, ReportSDKException {

		byte[] reportData = reportService.generateTestReport1(dueDays, customerNum, rmName, groupNum, customerName, sccStatus, footText);

		// Return the PDF as a response
		HttpHeaders headers = new HttpHeaders();
		headers.add("Content-Disposition", "inline; filename=report1-local.pdf");

		return ResponseEntity.ok()
				.headers(headers)
				.body(reportData);
	}

}
