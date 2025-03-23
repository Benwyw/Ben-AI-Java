package com.benwyw.bot.service;

import com.crystaldecisions.sdk.occa.report.application.OpenReportOptions;
import com.crystaldecisions.sdk.occa.report.application.ReportClientDocument;
import com.crystaldecisions.sdk.occa.report.exportoptions.ReportExportFormat;
import com.crystaldecisions.sdk.occa.report.lib.ReportSDKException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.*;

@Slf4j
@Service
public class ReportService {

	private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

	public byte[] generateTestReport1(String dueDays, String customerNum, String rmName, String groupNum, String customerName, String sccStatus, String footText) throws IOException, ReportSDKException {
		//		try {
		// Load the .rpt file as a resource
		ClassPathResource resource = new ClassPathResource("report/GeneralRpt.rpt");

		// Create a temporary file
		File tempFile = File.createTempFile("GeneralRpt", ".rpt");
		tempFile.deleteOnExit(); // Ensure the file is deleted after the application exits

		// Copy the .rpt file to the temporary file
		try (InputStream inputStream = resource.getInputStream();
			 FileOutputStream outputStream = new FileOutputStream(tempFile)) {
			byte[] buffer = new byte[1024];
			int bytesRead;
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead); // Corrected the offset and length
			}
		}

		// Log the absolute path of the temporary file
		String reportPath = tempFile.getAbsolutePath();
		logger.info("Temporary report file path: {}", reportPath);

		// Open the report using ReportClientDocument
		ReportClientDocument reportClientDocument = new ReportClientDocument();
		reportClientDocument.open(reportPath, OpenReportOptions._openAsReadOnly);

		// Set parameter fields
		reportClientDocument.getDataDefController().getParameterFieldController()
				.setCurrentValue("", "DueDays", dueDays);
		reportClientDocument.getDataDefController().getParameterFieldController()
				.setCurrentValue("", "CustomerNum", customerNum);
		reportClientDocument.getDataDefController().getParameterFieldController()
				.setCurrentValue("", "RmName", rmName);
		reportClientDocument.getDataDefController().getParameterFieldController()
				.setCurrentValue("", "GroupNum", groupNum);
		reportClientDocument.getDataDefController().getParameterFieldController()
				.setCurrentValue("", "CustomerName", customerName);
		reportClientDocument.getDataDefController().getParameterFieldController()
				.setCurrentValue("", "SccStatus", sccStatus);
		reportClientDocument.getDataDefController().getParameterFieldController()
				.setCurrentValue("", "FootText", footText);

		// Export the report to PDF
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		// DB?
//			reportClientDocument.getDatabaseController().tableLocation
//			reportClientDocument.getDatabaseController().logon("username", "password");
//			reportClientDocument.getDatabaseController().setTableLocation("TableName", "ServerName");


		reportClientDocument.getPrintOutputController().export(ReportExportFormat.PDF, outputStream);

		// Close the document and delete the file
		reportClientDocument.close();
		if (tempFile.exists() && tempFile.delete()) {
			logger.info("Temporary file deleted successfully.");
		} else {
			logger.warn("Failed to delete temporary file.");
		}

//		// Return the PDF as a response
//		HttpHeaders headers = new HttpHeaders();
//		headers.add("Content-Disposition", "inline; filename=report.pdf");

//		return ResponseEntity.ok()
//				.headers(headers)
//				.body(outputStream.toByteArray());

		return outputStream.toByteArray();

//		} catch (Exception e) {
//			e.printStackTrace();
//			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//		}
	}

	public byte[] generateTestReport2(String userName) throws IOException, ReportSDKException {
		//		try {
		// Load the .rpt file as a resource
		ClassPathResource resource = new ClassPathResource("report/CrystalRpt.rpt");

		// Create a temporary file
		File tempFile = File.createTempFile("CrystalRpt", ".rpt");
		tempFile.deleteOnExit(); // Ensure the file is deleted after the application exits

		// Copy the .rpt file to the temporary file
		try (InputStream inputStream = resource.getInputStream();
			 FileOutputStream outputStream = new FileOutputStream(tempFile)) {
			byte[] buffer = new byte[1024];
			int bytesRead;
			while ((bytesRead = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, bytesRead); // Corrected the offset and length
			}
		}

		// Log the absolute path of the temporary file
		String reportPath = tempFile.getAbsolutePath();
		logger.info("Temporary report file path: {}", reportPath);

		// Open the report using ReportClientDocument
		ReportClientDocument reportClientDocument = new ReportClientDocument();
		reportClientDocument.open(reportPath, OpenReportOptions._openAsReadOnly);

		// Set parameter fields
		reportClientDocument.getDataDefController().getParameterFieldController()
				.setCurrentValue("", "UserName", userName);

		// Export the report to PDF
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		// DB?
//			reportClientDocument.getDatabaseController().tableLocation
//			reportClientDocument.getDatabaseController().logon("username", "password");
//			reportClientDocument.getDatabaseController().setTableLocation("TableName", "ServerName");


		reportClientDocument.getPrintOutputController().export(ReportExportFormat.PDF, outputStream);

		// Close the document and delete the file
		reportClientDocument.close();
		if (tempFile.exists() && tempFile.delete()) {
			logger.info("Temporary file deleted successfully.");
		} else {
			logger.warn("Failed to delete temporary file.");
		}

		// Return the PDF as a response
//		HttpHeaders headers = new HttpHeaders();
//		headers.add("Content-Disposition", "inline; filename=report.pdf");
//
//		return ResponseEntity.ok()
//				.headers(headers)
//				.body(outputStream.toByteArray());

		return outputStream.toByteArray();

//		} catch (Exception e) {
//			e.printStackTrace();
//			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//		}
	}
}
