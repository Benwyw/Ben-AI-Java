package com.benwyw.bot.controller.web;

import com.benwyw.bot.service.FileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("file")
@Profile("local")
public class FileController {
	
	@Autowired
	private FileService fileService;

	@GetMapping("/createDirectory")
	@Profile("local")
	public String createDirectory(@RequestParam(required = false) String date) throws IOException {
		return fileService.createDirectory(date);
	}

	@GetMapping("/copyFilesOnly")
	@Profile("local")
	public void copyFilesOnly(@RequestParam(required = true) String targetDirectory) throws IOException {
		fileService.copyFilesOnly(targetDirectory);
	}

	@Deprecated
	@GetMapping("/unzipAndCopyFiles")
	@Profile("local")
	public void unzipAndCopyFiles(@RequestParam(required = true) String targetDirectory) throws IOException {
		fileService.unzipAndCopyFiles(targetDirectory);
	}

}
