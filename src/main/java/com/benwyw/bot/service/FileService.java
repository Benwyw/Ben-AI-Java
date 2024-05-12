package com.benwyw.bot.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
@Profile("local")
public class FileService {

	@Value("${app.deployment.path}")
	private String deploymentPath;

	public String createDirectory(String date) {
		String folderName = date != null ? date : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
		Path path = Paths.get(deploymentPath, folderName);

		try {
			if (!Files.exists(path)) {
				Files.createDirectories(path);
				log.info("Directory created: " + path);
			} else {
				log.error("Directory already exists: " + path);
			}
		} catch (Exception e) {
			log.error("Error occurred while creating directory: " + path, e);
		} finally {
			return path.toString();
		}
	}

//	public void copyFiles(String targetDirectory) {
//		Path sourceDirectory = Paths.get(System.getProperty("user.dir"), "deployment");
//		Path destinationDirectory = Paths.get(targetDirectory);
//
//		try (DirectoryStream<Path> stream = Files.newDirectoryStream(sourceDirectory)) {
//			for (Path file: stream) {
//				Path destinationPath = destinationDirectory.resolve(file.getFileName());
//				Files.copy(file, destinationPath, StandardCopyOption.REPLACE_EXISTING);
//				log.info("Copied file: " + file + " to " + destinationPath);
//			}
//		} catch (IOException e) {
//			log.error("Error occurred while copying files from " + sourceDirectory + " to " + destinationDirectory, e);
//		}
//	}

	public void copyFilesOnly(String targetDirectory) {
		Path sourceDirectory = Paths.get(System.getProperty("user.dir"), "deployment");
		Path destinationDirectory = Paths.get(targetDirectory);

		try {
			Files.walkFileTree(sourceDirectory, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					Path targetPath = destinationDirectory.resolve(sourceDirectory.relativize(dir));
					if(!Files.exists(targetPath)){
						Files.createDirectory(targetPath);
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.copy(file, destinationDirectory.resolve(sourceDirectory.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
					log.info("Copied file: " + file + " to " + destinationDirectory.resolve(sourceDirectory.relativize(file)));
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			log.error("Error occurred while copying files from " + sourceDirectory + " to " + destinationDirectory, e);
		}
	}

	@Deprecated
	public void unzipAndCopyFiles(String targetDirectory) {
		Path sourceDirectory = Paths.get(System.getProperty("user.dir"), "deployment");
		Path tempDirectory = Paths.get(System.getProperty("java.io.tmpdir"), "deployment");

		try {
			Files.walkFileTree(sourceDirectory, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (file.toString().endsWith(".zip")) {
						unzip(file, tempDirectory.resolve(sourceDirectory.relativize(file)).toString());
					} else {
						Files.copy(file, tempDirectory.resolve(sourceDirectory.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			log.error("Error occurred while unzipping and copying files from " + sourceDirectory + " to " + tempDirectory, e);
		}

		copyFiles(tempDirectory, Paths.get(targetDirectory));
	}

	private void unzip(Path zipFile, String outputDirectory) throws IOException {
		try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipFile))) {
			ZipEntry entry = zis.getNextEntry();
			while (entry != null) {
				Path entryPath = Paths.get(outputDirectory, entry.getName());
				if (entry.isDirectory()) {
					Files.createDirectories(entryPath);
				} else {
					Files.createDirectories(entryPath.getParent());
					Files.copy(zis, entryPath, StandardCopyOption.REPLACE_EXISTING);
				}
				entry = zis.getNextEntry();
			}
		}
	}

	@Deprecated
	private void copyFiles(Path sourceDirectory, Path destinationDirectory) {
		try {
			Files.walkFileTree(sourceDirectory, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					Path targetPath = destinationDirectory.resolve(sourceDirectory.relativize(dir));
					if(!Files.exists(targetPath)){
						Files.createDirectories(targetPath);
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.copy(file, destinationDirectory.resolve(sourceDirectory.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
					log.info("Copied file: " + file + " to " + destinationDirectory.resolve(sourceDirectory.relativize(file)));
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			log.error("Error occurred while copying files from " + sourceDirectory + " to " + destinationDirectory, e);
		}
	}
}
