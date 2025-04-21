/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.payment.util;

import java.text.DecimalFormat;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com 
 */
@Service
@RequiredArgsConstructor
public class ReceiptNumberGenerator {

	 private static final String PREFIX = "INV-EP";
	 private static final DecimalFormat FORMAT = new DecimalFormat("000000");

//	 @Query("SELECT COALESCE(MAX(CAST(SUBSTRING(z.invoiceNumber, 7, LENGTH(z.invoiceNumber) - 6) AS int)), 0) " +
//	            "FROM ZohoInvoice z WHERE z.invoiceNumber LIKE 'INV-EP%'")
//	    Long findMaxInvoiceNumber();
	 
	@Transactional
    public String generateInvoiceNumber() {
//        Long lastInvoiceNumber = invoiceRepository.findMaxInvoiceNumber();
//        long nextNumber = (lastInvoiceNumber == null) ? 1 : lastInvoiceNumber + 1;
		long nextNumber = 1l;
        return PREFIX + FORMAT.format(nextNumber);
    }
}
