package com.kittyp.common.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.kittyp.common.dto.FileUploadRequest;
import com.kittyp.common.exception.CustomException;

@Service
public class ImageUploadSanitizer {

	static {
		System.setProperty("java.awt.headless", "true");
	}

	static final String UNSAFE_IMAGE = "Unsafe or invalid image";
	static final String UNSAFE_FILE = "Unsafe or invalid file";

	private static final int MAX_SIDE = 4096;
	private static final long MAX_PIXELS = 20_000_000L;
	private static final byte[] JPEG_MAGIC = { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF };
	private static final byte[] PNG_MAGIC = { (byte) 0x89, 0x50, 0x4E, 0x47 };
	private static final byte[] GIF87 = "GIF87a".getBytes(StandardCharsets.US_ASCII);
	private static final byte[] GIF89 = "GIF89a".getBytes(StandardCharsets.US_ASCII);
	private static final byte[] RIFF = "RIFF".getBytes(StandardCharsets.US_ASCII);
	private static final byte[] WEBP = "WEBP".getBytes(StandardCharsets.US_ASCII);
	private static final byte[] PDF_MAGIC = "%PDF-".getBytes(StandardCharsets.US_ASCII);

	public FileUploadRequest sanitize(String fileName, byte[] data, String contentType) {
		if (data == null || data.length == 0) {
			throw new CustomException(UNSAFE_FILE, HttpStatus.BAD_REQUEST);
		}
		String safeName = fileName == null || fileName.isBlank() ? "upload.bin" : fileName;
		String type = detectType(data, contentType, safeName);

		if ("application/pdf".equals(type)) {
			if (!isPdf(data)) {
				throw new CustomException("This is not a valid PDF", HttpStatus.BAD_REQUEST);
			}
			return new FileUploadRequest(withExtension(safeName, ".pdf"), data, "application/pdf");
		}

		if (!isAllowedImageType(type)) {
			throw new CustomException(UNSAFE_IMAGE, HttpStatus.BAD_REQUEST);
		}
		if (!hasValidImageMagic(data) && looksLikeMarkupOrScript(data)) {
			throw new CustomException(UNSAFE_IMAGE, HttpStatus.BAD_REQUEST);
		}

		BufferedImage image;
		try {
			image = ImageIO.read(new ByteArrayInputStream(data));
		} catch (IOException e) {
			throw new CustomException(UNSAFE_IMAGE, HttpStatus.BAD_REQUEST, e);
		}
		if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
			throw new CustomException(UNSAFE_IMAGE, HttpStatus.BAD_REQUEST);
		}
		long pixels = (long) image.getWidth() * (long) image.getHeight();
		if (image.getWidth() > MAX_SIDE || image.getHeight() > MAX_SIDE || pixels > MAX_PIXELS) {
			throw new CustomException(UNSAFE_IMAGE, HttpStatus.BAD_REQUEST);
		}

		boolean jpeg = "image/jpeg".equals(type);
		try {
			byte[] recoded = jpeg ? writeJpeg(image) : writePng(image);
			String ext = jpeg ? ".jpg" : ".png";
			String outType = jpeg ? "image/jpeg" : "image/png";
			return new FileUploadRequest(withExtension(safeName, ext), recoded, outType);
		} catch (IOException e) {
			throw new CustomException(UNSAFE_IMAGE, HttpStatus.BAD_REQUEST, e);
		}
	}

	/**
	 * Trust file bytes over the browser Content-Type. A JPEG named .png or a PDF sent as
	 * octet-stream is common on Windows and must not be rejected as an "unsafe image".
	 */
	private static String detectType(byte[] data, String contentType, String fileName) {
		if (isPdf(data)) {
			return "application/pdf";
		}
		if (startsWith(data, JPEG_MAGIC)) {
			return "image/jpeg";
		}
		if (startsWith(data, PNG_MAGIC)) {
			return "image/png";
		}
		if (startsWith(data, GIF87) || startsWith(data, GIF89)) {
			return "image/gif";
		}
		if (startsWith(data, RIFF) && data.length >= 12 && regionEquals(data, 8, WEBP)) {
			return "image/webp";
		}
		return normalizeType(contentType, fileName);
	}

	private static String normalizeType(String contentType, String fileName) {
		String type = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT).trim();
		int semi = type.indexOf(';');
		if (semi >= 0) {
			type = type.substring(0, semi).trim();
		}
		return switch (type) {
		case "image/jpg", "image/pjpeg" -> "image/jpeg";
		case "image/x-png" -> "image/png";
		case "application/x-pdf", "application/acrobat" -> "application/pdf";
		case "", "application/octet-stream", "binary/octet-stream" -> typeFromFileName(fileName);
		default -> type;
		};
	}

	private static String typeFromFileName(String fileName) {
		String lower = fileName.toLowerCase(Locale.ROOT);
		if (lower.endsWith(".pdf")) {
			return "application/pdf";
		}
		if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
			return "image/jpeg";
		}
		if (lower.endsWith(".png")) {
			return "image/png";
		}
		if (lower.endsWith(".gif")) {
			return "image/gif";
		}
		if (lower.endsWith(".webp")) {
			return "image/webp";
		}
		return "";
	}

	private static boolean isPdf(byte[] data) {
		int i = 0;
		if (data.length >= 3 && (data[0] & 0xFF) == 0xEF && (data[1] & 0xFF) == 0xBB && (data[2] & 0xFF) == 0xBF) {
			i = 3;
		}
		while (i < data.length && i < 1024) {
			byte b = data[i];
			if (b != ' ' && b != '\n' && b != '\r' && b != '\t') {
				break;
			}
			i++;
		}
		return regionEquals(data, i, PDF_MAGIC);
	}

	private static boolean isAllowedImageType(String type) {
		return "image/jpeg".equals(type) || "image/png".equals(type) || "image/webp".equals(type)
				|| "image/gif".equals(type);
	}

	private static boolean hasValidImageMagic(byte[] data) {
		return startsWith(data, JPEG_MAGIC) || startsWith(data, PNG_MAGIC) || startsWith(data, GIF87)
				|| startsWith(data, GIF89) || (startsWith(data, RIFF) && data.length >= 12 && regionEquals(data, 8, WEBP));
	}

	private static boolean looksLikeMarkupOrScript(byte[] data) {
		int n = Math.min(data.length, 4096);
		String head = new String(data, 0, n, StandardCharsets.US_ASCII).toLowerCase(Locale.ROOT);
		return head.contains("<html") || head.contains("<script") || head.contains("<?php") || head.contains("<?xml")
				|| head.contains("<svg");
	}

	private static boolean startsWith(byte[] data, byte[] prefix) {
		return regionEquals(data, 0, prefix);
	}

	private static boolean regionEquals(byte[] data, int offset, byte[] prefix) {
		if (data.length < offset + prefix.length) {
			return false;
		}
		for (int i = 0; i < prefix.length; i++) {
			if (data[offset + i] != prefix[i]) {
				return false;
			}
		}
		return true;
	}

	private static byte[] writeJpeg(BufferedImage source) throws IOException {
		BufferedImage rgb = source;
		if (source.getType() != BufferedImage.TYPE_INT_RGB) {
			rgb = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
			Graphics2D g = rgb.createGraphics();
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, source.getWidth(), source.getHeight());
			g.drawImage(source, 0, 0, null);
			g.dispose();
		}
		Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
		if (!writers.hasNext()) {
			throw new IOException("No JPEG writer");
		}
		ImageWriter writer = writers.next();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
			writer.setOutput(ios);
			ImageWriteParam param = writer.getDefaultWriteParam();
			if (param.canWriteCompressed()) {
				param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
				param.setCompressionQuality(0.9f);
			}
			writer.write(null, new IIOImage(rgb, null, null), param);
		} finally {
			writer.dispose();
		}
		return baos.toByteArray();
	}

	private static byte[] writePng(BufferedImage source) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		if (!ImageIO.write(source, "png", baos)) {
			throw new IOException("No PNG writer");
		}
		return baos.toByteArray();
	}

	private static String withExtension(String fileName, String ext) {
		int slash = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
		String base = slash >= 0 ? fileName.substring(slash + 1) : fileName;
		int dot = base.lastIndexOf('.');
		if (dot > 0) {
			base = base.substring(0, dot);
		}
		if (base.isBlank()) {
			base = "photo";
		}
		return base + ext;
	}
}
