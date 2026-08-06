/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.payment.util;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.itextpdf.io.source.ByteArrayOutputStream;
import com.kittyp.payment.model.InvoiceData;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author rrohan419@gmail.com
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PdfGenerator {

	private final SpringTemplateEngine thymeleaf;

	/** Font family registered for Unicode (includes Indian Rupee ₹). */
	public static final String UNICODE_FONT_FAMILY = "KittypInvoice";

	public byte[] generateInvoicePdf(InvoiceData data) {
		return generatePdf("invoice-template.html", "invoice", data);
	}

	public byte[] generateTreatmentInvoicePdf(Object data) {
		return generatePdf("treatment-invoice-template.html", "invoice", data);
	}

	public byte[] generatePdf(String templateName, String variableName, Object data) {
		Context ctx = new Context();
		ctx.setVariable(variableName, data);
		String html = thymeleaf.process(templateName, ctx);

		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			PdfRendererBuilder builder = new PdfRendererBuilder();
			builder.useFastMode();
			registerUnicodeFont(builder);
			builder.withHtmlContent(html, null);
			builder.toStream(out);
			builder.run();
			return out.toByteArray();
		} catch (Exception e) {
			throw new RuntimeException("PDF generation failed", e);
		}
	}

	/**
	 * Helvetica cannot render ₹ (shows as #). Register a Unicode-capable font.
	 */
	private void registerUnicodeFont(PdfRendererBuilder builder) {
		File fontFile = resolveUnicodeFontFile();
		if (fontFile == null) {
			log.warn("No Unicode font found for PDF; ₹ may render incorrectly. Add fonts/NotoSans-Regular.ttf to resources.");
			return;
		}
		try {
			builder.useFont(fontFile, UNICODE_FONT_FAMILY);
		} catch (Exception e) {
			log.warn("Failed to register Unicode PDF font from {}: {}", fontFile, e.getMessage());
		}
	}

	private File resolveUnicodeFontFile() {
		// 1) Bundled classpath font (preferred for all environments)
		File bundled = copyClasspathFont("fonts/NotoSans-Regular.ttf");
		if (bundled != null) {
			return bundled;
		}
		bundled = copyClasspathFont("fonts/DejaVuSans.ttf");
		if (bundled != null) {
			return bundled;
		}

		// 2) Common OS fonts that include U+20B9 (₹)
		String[] candidates = {
				"C:/Windows/Fonts/Nirmala.ttf",
				"C:/Windows/Fonts/NirmalaS.ttf",
				"C:/Windows/Fonts/arial.ttf",
				"C:/Windows/Fonts/seguiui.ttf",
				"/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
				"/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf",
				"/System/Library/Fonts/Supplemental/Arial Unicode.ttf"
		};
		for (String path : candidates) {
			Path p = Paths.get(path);
			if (Files.isRegularFile(p)) {
				return p.toFile();
			}
		}
		return null;
	}

	private File copyClasspathFont(String classpathLocation) {
		try {
			ClassPathResource resource = new ClassPathResource(classpathLocation);
			if (!resource.exists()) {
				return null;
			}
			Path temp = Files.createTempFile("kittyp-font-", ".ttf");
			temp.toFile().deleteOnExit();
			try (InputStream in = resource.getInputStream()) {
				Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
			}
			return temp.toFile();
		} catch (Exception e) {
			return null;
		}
	}
}
