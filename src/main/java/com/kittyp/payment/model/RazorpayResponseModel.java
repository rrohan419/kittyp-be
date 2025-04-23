/**
 * @author rrohan419@gmail.com
 */
package com.kittyp.payment.model;

import lombok.Data;
import java.util.Map;

/**
 * @author rrohan419@gmail.com 
 */
@Data
public class RazorpayResponseModel {
    private String entity;
    private String account_id;
    private String event;
    private String[] contains;
    private Payload payload;
    private long created_at;

    @Data
    public static class Payload {
        private PaymentWrapper payment;
    }

    @Data
    public static class PaymentWrapper {
        private PaymentEntity entity;
    }

    @Data
    public static class PaymentEntity {
        private String id;
        private String entity;
        private int amount;
        private String currency;
        private String status;
        private String order_id;
        private String invoice_id;
        private boolean international;
        private String method;
        private int amount_refunded;
        private String refund_status;
        private boolean captured;
        private String description;
        private String card_id;
        private String bank;
        private String wallet;
        private String vpa;
        private String email;
        private String contact;
        private Map<String, String> notes;
        private Integer fee;
        private Integer tax;
        private String error_code;
        private String error_description;
        private String error_source;
        private String error_step;
        private String error_reason;
        private AcquirerData acquirer_data;
        private long created_at;
        private Upi upi;
    }

    @Data
    public static class AcquirerData {
        private String rrn;
        private String upi_transaction_id;
    }

    @Data
    public static class Upi {
        private String vpa;
    }
}

