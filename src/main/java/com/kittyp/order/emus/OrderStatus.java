/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.order.emus;

/**
 * @author rrohan419@gmail.com 
 */
public enum OrderStatus{
	CREATED,        // When order is created
    SUCCESSFULL,     // When payment is authorized (especially for UPI, Netbanking)
    DELIVERED,       // Final successful DELIVERY state
    FAILED,         // Payment failed
    REFUNDED,       // If refund happens
    CANCELLED,      // If order is manually cancelled
	REFUND_INITIATED;
    
    public static OrderStatus fromRazorpayStatus(String status) {
        switch (status.toLowerCase().trim()) {
            case "created": return CREATED;
            case "captured", "authorized": return SUCCESSFULL;
//            case "authorized": return SUCCESSFULL;
//            case "captured": return CAPTURED;
            case "failed": return FAILED;
            case "cancelled": return CANCELLED;
            case "refund_initiated": return REFUND_INITIATED;
            case "refunded","partially_refunded": return REFUNDED;
            default: throw new IllegalArgumentException("Unknown Razorpay status: " + status);
        }
    }
}
