package com.kittyp.common.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.kittyp.common.dto.FileUploadRequest;
import com.kittyp.common.exception.CustomException;

class ImageUploadSanitizerTest {

	private ImageUploadSanitizer sanitizer;

	@BeforeEach
	void setUp() {
		sanitizer = new ImageUploadSanitizer();
	}

	@Test
	void validJpegIsAcceptedAndReencodedWithoutExifComment() throws Exception {
		byte[] jpeg = writeImage("jpeg", 8, 8);
		byte[] withComment = spliceJpegComment(jpeg, "EXIF-HACK-PAYLOAD");

		FileUploadRequest out = sanitizer.sanitize("pet.jpg", withComment, "image/jpeg");

		assertEquals("image/jpeg", out.getContentType());
		assertTrue(out.getFileName().endsWith(".jpg"));
		assertTrue(startsWithJpeg(out.getData()));
		assertFalse(new String(out.getData(), StandardCharsets.ISO_8859_1).contains("EXIF-HACK-PAYLOAD"));
		assertTrue(ImageIO.read(new ByteArrayInputStream(out.getData())).getWidth() > 0);
	}

	@Test
	void validPngIsAccepted() throws Exception {
		byte[] png = writeImage("png", 4, 4);

		FileUploadRequest out = sanitizer.sanitize("pet.png", png, "image/png");

		assertEquals("image/png", out.getContentType());
		assertTrue(out.getFileName().endsWith(".png"));
		assertTrue(ImageIO.read(new ByteArrayInputStream(out.getData())).getHeight() > 0);
	}

	@Test
	void jpegWithTrailingScriptIsReencodedWithoutScript() throws Exception {
		byte[] jpeg = writeImage("jpeg", 6, 6);
		byte[] polyglot = concat(jpeg, "<?php system('id'); ?><script>alert(1)</script>".getBytes(StandardCharsets.US_ASCII));

		FileUploadRequest out = sanitizer.sanitize("pet.jpg", polyglot, "image/jpeg");

		String recoded = new String(out.getData(), StandardCharsets.ISO_8859_1);
		assertFalse(recoded.contains("<?php"));
		assertFalse(recoded.contains("<script"));
		assertTrue(startsWithJpeg(out.getData()));
	}

	@Test
	void htmlNamedAsJpegIsRejected() {
		byte[] html = "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.US_ASCII);

		CustomException ex = assertThrows(CustomException.class,
				() -> sanitizer.sanitize("photo.jpg", html, "image/jpeg"));

		assertEquals(ImageUploadSanitizer.UNSAFE_IMAGE, ex.getMessage());
		assertEquals(HttpStatus.BAD_REQUEST, ex.getHttpStatus());
	}

	@Test
	void svgIsRejected() {
		byte[] svg = "<?xml version=\"1.0\"?><svg xmlns=\"http://www.w3.org/2000/svg\"></svg>"
				.getBytes(StandardCharsets.US_ASCII);

		CustomException ex = assertThrows(CustomException.class,
				() -> sanitizer.sanitize("pet.svg", svg, "image/svg+xml"));

		assertEquals(ImageUploadSanitizer.UNSAFE_IMAGE, ex.getMessage());
	}

	@Test
	void oversizePixelBufferIsRejected() throws Exception {
		byte[] tall = writeImage("png", 4097, 8);

		CustomException ex = assertThrows(CustomException.class,
				() -> sanitizer.sanitize("huge.png", tall, "image/png"));

		assertEquals(ImageUploadSanitizer.UNSAFE_IMAGE, ex.getMessage());
	}

	@Test
	void jpegLabeledAsPngIsAcceptedAsJpeg() throws Exception {
		byte[] jpeg = writeImage("jpeg", 6, 6);

		FileUploadRequest out = sanitizer.sanitize("scan.png", jpeg, "image/png");

		assertEquals("image/jpeg", out.getContentType());
		assertTrue(out.getFileName().endsWith(".jpg"));
	}

	@Test
	void pdfIsAcceptedEvenWhenBrowserSendsOctetStream() {
		byte[] pdf = "%PDF-1.4\n1 0 obj<<>>endobj\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);

		FileUploadRequest out = sanitizer.sanitize("degree.pdf", pdf, "application/octet-stream");

		assertEquals("application/pdf", out.getContentType());
		assertTrue(out.getFileName().endsWith(".pdf"));
	}

	@Test
	void pdfWithUtf8BomIsAccepted() {
		byte[] body = "%PDF-1.7\n%%EOF\n".getBytes(StandardCharsets.US_ASCII);
		byte[] pdf = new byte[3 + body.length];
		pdf[0] = (byte) 0xEF;
		pdf[1] = (byte) 0xBB;
		pdf[2] = (byte) 0xBF;
		System.arraycopy(body, 0, pdf, 3, body.length);

		FileUploadRequest out = sanitizer.sanitize("reg.pdf", pdf, "application/pdf");

		assertEquals("application/pdf", out.getContentType());
	}

	private static byte[] writeImage(String format, int width, int height) throws IOException {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = image.createGraphics();
		g.setColor(Color.ORANGE);
		g.fillRect(0, 0, width, height);
		g.dispose();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		assertTrue(ImageIO.write(image, format, baos));
		return baos.toByteArray();
	}

	private static byte[] spliceJpegComment(byte[] jpeg, String comment) {
		byte[] payload = comment.getBytes(StandardCharsets.US_ASCII);
		int len = payload.length + 2;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(jpeg, 0, 2);
		out.write(0xFF);
		out.write(0xFE);
		out.write((len >> 8) & 0xFF);
		out.write(len & 0xFF);
		out.writeBytes(payload);
		out.write(jpeg, 2, jpeg.length - 2);
		return out.toByteArray();
	}

	private static byte[] concat(byte[] head, byte[] tail) {
		byte[] out = new byte[head.length + tail.length];
		System.arraycopy(head, 0, out, 0, head.length);
		System.arraycopy(tail, 0, out, head.length, tail.length);
		return out;
	}

	private static boolean startsWithJpeg(byte[] data) {
		return data.length >= 3 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8 && (data[2] & 0xFF) == 0xFF;
	}
}
