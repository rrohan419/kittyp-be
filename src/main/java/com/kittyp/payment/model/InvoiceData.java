/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.payment.model;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import com.kittyp.order.entity.Order;
import com.kittyp.order.entity.OrderItem;
import com.kittyp.user.entity.User;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author rrohan419@gmail.com 
 */
@Data
public class InvoiceData {

	private String invoiceNumber;
    private String date;
    private String customerName;
    private List<Item> items;
    private BigDecimal subtotal;
    private BigDecimal shippingCost;
    private BigDecimal tax;
    private BigDecimal serviceCharges;
    private BigDecimal total;

    @Data
    @AllArgsConstructor
    public static class Item {
        private String name;
        private int quantity;
        private BigDecimal price;
        private BigDecimal total;
    }
    
//    public InvoiceData from(Order order) {
//    	
//    	this.invoiceNumber = order.getOrderNumber();
//    	this.date = order.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE);
//    	this.customerName = order.getUser()
//    	
//    	List<OrderItem> orderItems = order.getOrderItems();
//    }
    
    
    public InvoiceData from(Order order) {
        this.invoiceNumber = order.getOrderNumber();
        this.date = order.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE);

        // Trigger loading of lazy user (ensure JPA session is open)
        User user = order.getUser();
        this.customerName = user.getFirstName()+" "+user.getLastName(); // Adjust based on your field

        List<Item> itemList = order.getOrderItems().stream().map(oi -> {
            BigDecimal itemTotal = oi.getPrice().multiply(BigDecimal.valueOf(oi.getQuantity()));
            return new Item(
                oi.getProduct().getName(), // assuming this relation exists
                oi.getQuantity(),
                oi.getPrice(),
                itemTotal
            );
        }).toList();

        this.items = itemList;
        this.subtotal = itemList.stream()
                                .map(Item::getTotal)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
        this.shippingCost = order.getTaxes().getShippingCharges();
        this.tax = order.getTaxes().getOtherTax();
        this.serviceCharges = order.getTaxes().getServiceCharge();

        this.total = subtotal.add(shippingCost).add(tax).add(serviceCharges);

        return this;
    }

}
