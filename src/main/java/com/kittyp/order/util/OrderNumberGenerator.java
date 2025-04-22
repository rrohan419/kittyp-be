/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.order.util;

import java.text.DecimalFormat;

import org.springframework.stereotype.Service;

import com.kittyp.order.repository.OrderRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * @author rrohan419@gmail.com 
 */
@Service
@RequiredArgsConstructor
public class OrderNumberGenerator {
	private final OrderRepository orderRepository;
	private static final String PREFIX = "IND-KP";
    private static final DecimalFormat FORMAT = new DecimalFormat("000000");

    /**
     * @author rrohan419@gmail.com 
     */
    @Transactional
    public String generateInvoiceNumber() {
        Long lastInvoiceNumber = orderRepository.findMaxOrderNumber();
        long nextNumber = (lastInvoiceNumber == null) ? 1 : lastInvoiceNumber + 1;
        return PREFIX + FORMAT.format(nextNumber);
    }
}
