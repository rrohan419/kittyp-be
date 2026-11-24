package com.kittyp.notification.entity;

import com.kittyp.common.entity.BaseEntity;
import com.kittyp.notification.enums.MessageChannel;
import com.kittyp.notification.enums.MessageTenantType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "messaging_credit_ledger", uniqueConstraints = @UniqueConstraint(name = "uk_message_ledger_idem", columnNames = {
        "tenant_type", "tenant_id", "channel", "idempotency_key" }))
@Data
@EqualsAndHashCode(callSuper = true)
public class MessageLedgerEntry extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(name = "tenant_type", nullable = false, length = 16)
    private MessageTenantType tenantType;
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MessageChannel channel;
    @Column(name = "entry_type", nullable = false, length = 24)
    private String entryType;
    @Column(name = "credit_delta", nullable = false)
    private Long creditDelta;
    @Column(name = "message_uuid", length = 36)
    private String messageUuid;
    @Column(name = "reference_id", length = 120)
    private String referenceId;
    @Column(name = "idempotency_key", nullable = false, length = 140)
    private String idempotencyKey;
    @Column(length = 3)
    private String currency = "INR";
    @Column(name = "amount_ex_tax", precision = 12, scale = 2)
    private java.math.BigDecimal amountExTax;
    @Column(name = "tax_amount", precision = 12, scale = 2)
    private java.math.BigDecimal taxAmount;
}
