package com.benwyw.bot.service;

import lombok.extern.slf4j.Slf4j;
import org.imgscalr.Scalr;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

@Slf4j
@Service
public class ImageService {

//	@Autowired
//	@SuppressWarnings("unused")
//	private ShardManager shardManager;

	/**
	 * Take in image file then return aHash
	 * @param base64Image String
	 * @return String
	 */
	public ResponseEntity<String> calculateAHash(@RequestBody String base64Image) {
		try {
			// Decode the base64 image string
			byte[] imageBytes = Base64.getDecoder().decode(base64Image);

			// Convert the image bytes to BufferedImage
			ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
			BufferedImage image = ImageIO.read(bis);

			// Resize image to 8x8 pixels
			BufferedImage resizedImage = Scalr.resize(image, Scalr.Method.AUTOMATIC, Scalr.Mode.FIT_EXACT, 8, 8);

			// Convert the resized image to grayscale
			BufferedImage grayscaleImage = new BufferedImage(8, 8, BufferedImage.TYPE_BYTE_GRAY);
			grayscaleImage.getGraphics().drawImage(resizedImage, 0, 0, null);

			// Calculate the aHash
			long aHash = calculateAHash(grayscaleImage);

			// Convert the aHash to a string
			String aHashString = Long.toHexString(aHash);

			return ResponseEntity.ok(aHashString);
		} catch (IOException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	private long calculateAHash(BufferedImage image) {
		long hash = 0;
		int width = image.getWidth();
		int height = image.getHeight();

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				hash = hash << 1;
				int pixel = image.getRGB(x, y);
				int luminance = getLuminance(pixel);
				if (luminance < getAverageLuminance(image)) {
					hash |= 1;
				}
			}
		}

		return hash;
	}

	private int getLuminance(int pixel) {
		int red = (pixel >> 16) & 0xFF;
		int green = (pixel >> 8) & 0xFF;
		int blue = pixel & 0xFF;
		return (int) (0.299 * red + 0.587 * green + 0.114 * blue);
	}

	private int getAverageLuminance(BufferedImage image) {
		int width = image.getWidth();
		int height = image.getHeight();
		int totalLuminance = 0;

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int pixel = image.getRGB(x, y);
				totalLuminance += getLuminance(pixel);
			}
		}

		return totalLuminance / (width * height);
	}
}
