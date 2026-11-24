package com.kittyp.notification.entity;

import java.math.BigDecimal;
import com.kittyp.common.entity.BaseEntity;
import com.kittyp.notification.enums.MessageChannel;
import com.kittyp.notification.enums.MessageTenantType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "messaging_purchases", uniqueConstraints = {
        @UniqueConstraint(name = "uk_message_purchase_order", columnNames = "razorpay_order_id"),
        @UniqueConstraint(name = "uk_message_purchase_payment", columnNames = "razorpay_payment_id") })
@Data
@EqualsAndHashCode(callSuper = true)
public class MessagePurchase extends BaseEntity {
    @Column(nullable = false, unique = true, length = 36)
    private String uuid;
    @Enumerated(EnumType.STRING)
    @Column(name = "tenant_type", nullable = false, length = 16)
    private MessageTenantType tenantType;
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MessageChannel channel;
    @Column(name = "credits", nullable = false)
    private Integer credits;
    @Column(name = "amount_ex_tax", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountExTax;
    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount;
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;
    @Column(name = "razorpay_order_id", length = 100)
    private String razorpayOrderId;
    @Column(name = "razorpay_payment_id", length = 100)
    private String razorpayPaymentId;
    @Column(nullable = false, length = 16)
    private String status = "CREATED";
}
