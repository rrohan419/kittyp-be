/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.payment.util;

import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.itextpdf.io.source.ByteArrayOutputStream;
import com.kittyp.payment.model.InvoiceData;

import lombok.RequiredArgsConstructor;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;


/**
 * @author rrohan419@gmail.com 
 */
@Service
@RequiredArgsConstructor
public class PdfGenerator {

	private final SpringTemplateEngine thymeleaf;

    public byte[] generateInvoicePdf(InvoiceData data) {
        // Fill the template with data
        Context ctx = new Context();
        ctx.setVariable("invoice", data);
        String html = thymeleaf.process("invoice-template.html", ctx);

        // Convert HTML to PDF
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed", e);
        }
    }
}
