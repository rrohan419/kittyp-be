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
import java.util.Base64;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.itextpdf.io.source.ByteArrayOutputStream;
import com.kittyp.doctor.dto.TreatmentInvoiceData;
import com.kittyp.payment.model.InvoiceData;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle;
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

	private static final String KITTYP_LOGO_CLASSPATH = "static/invoice/kittyp-logo.png";

	private final SpringTemplateEngine thymeleaf;

	/** Font family registered for Unicode (includes Indian Rupee ₹). */
	public static final String UNICODE_FONT_FAMILY = "KittypInvoice";

	public byte[] generateInvoicePdf(InvoiceData data) {
		return generatePdf("invoice-template.html", "invoice", data);
	}

	public byte[] generateTreatmentInvoicePdf(Object data) {
		if (data instanceof TreatmentInvoiceData invoiceData) {
			if (invoiceData.getKittypLogoSrc() == null || invoiceData.getKittypLogoSrc().isBlank()) {
				invoiceData.setKittypLogoSrc(loadKittypLogoDataUri());
			}
		}
		return generatePdf("treatment-invoice-template.html", "invoice", data);
	}

	public byte[] generatePdf(String templateName, String variableName, Object data) {
		Context ctx = new Context();
		ctx.setVariable(variableName, data);
		String html = thymeleaf.process(templateName, ctx);

		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			PdfRendererBuilder builder = new PdfRendererBuilder();
			// Avoid useFastMode — it softens text/glyph rasterization on many viewers.
			registerUnicodeFonts(builder);
			builder.useDefaultPageSize(210, 297, PdfRendererBuilder.PageSizeUnits.MM);
			builder.withHtmlContent(html, null);
			builder.toStream(out);
			builder.run();
			return out.toByteArray();
		} catch (Exception e) {
			throw new RuntimeException("PDF generation failed", e);
		}
	}

	private String loadKittypLogoDataUri() {
		try {
			ClassPathResource resource = new ClassPathResource(KITTYP_LOGO_CLASSPATH);
			if (!resource.exists()) {
				log.warn("KittyP invoice logo missing at classpath:{}", KITTYP_LOGO_CLASSPATH);
				return null;
			}
			try (InputStream in = resource.getInputStream()) {
				byte[] bytes = in.readAllBytes();
				return "data:image/png;base64," + Base64.getEncoder().encodeToString(bytes);
			}
		} catch (Exception e) {
			log.warn("Failed to load KittyP invoice logo: {}", e.getMessage());
			return null;
		}
	}

	/**
	 * Helvetica cannot render ₹ (shows as #). Register regular + bold so titles are
	 * not faux-bold (which looks blurry in PDF viewers).
	 */
	private void registerUnicodeFonts(PdfRendererBuilder builder) {
		File regular = resolveUnicodeFontFile(false);
		if (regular == null) {
			log.warn("No Unicode font found for PDF; ₹ may render incorrectly. Add fonts/NotoSans-Regular.ttf to resources.");
			return;
		}
		try {
			builder.useFont(regular, UNICODE_FONT_FAMILY, 400, FontStyle.NORMAL, true);
			File bold = resolveUnicodeFontFile(true);
			if (bold != null) {
				builder.useFont(bold, UNICODE_FONT_FAMILY, 700, FontStyle.NORMAL, true);
			} else {
				// Prefer real bold; fall back to same file at weight 700 if needed.
				builder.useFont(regular, UNICODE_FONT_FAMILY, 700, FontStyle.NORMAL, true);
			}
		} catch (Exception e) {
			log.warn("Failed to register Unicode PDF fonts: {}", e.getMessage());
		}
	}

	private File resolveUnicodeFontFile(boolean bold) {
		if (bold) {
			File bundledBold = copyClasspathFont("fonts/NotoSans-Bold.ttf");
			if (bundledBold != null) {
				return bundledBold;
			}
			String[] boldCandidates = {
					"C:/Windows/Fonts/arialbd.ttf",
					"C:/Windows/Fonts/segoeuib.ttf",
					"C:/Windows/Fonts/NirmalaB.ttf",
					"/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
					"/usr/share/fonts/truetype/noto/NotoSans-Bold.ttf",
			};
			for (String path : boldCandidates) {
				Path p = Paths.get(path);
				if (Files.isRegularFile(p)) {
					return p.toFile();
				}
			}
			return null;
		}

		File bundled = copyClasspathFont("fonts/NotoSans-Regular.ttf");
		if (bundled != null) {
			return bundled;
		}
		bundled = copyClasspathFont("fonts/DejaVuSans.ttf");
		if (bundled != null) {
			return bundled;
		}

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
