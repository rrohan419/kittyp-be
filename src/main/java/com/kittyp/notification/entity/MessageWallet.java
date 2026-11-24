package com.kittyp.notification.entity;

import com.kittyp.common.entity.BaseEntity;
import com.kittyp.notification.enums.MessageChannel;
import com.kittyp.notification.enums.MessageTenantType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "messaging_wallets", uniqueConstraints = @UniqueConstraint(name = "uk_messaging_wallet_tenant_channel", columnNames = {
        "tenant_type", "tenant_id", "channel" }))
@Data
@EqualsAndHashCode(callSuper = true)
public class MessageWallet extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(name = "tenant_type", nullable = false, length = 16)
    private MessageTenantType tenantType;
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MessageChannel channel;
    @Column(name = "available_credits", nullable = false)
    private Long availableCredits = 0L;
    @Column(name = "reserved_credits", nullable = false)
    private Long reservedCredits = 0L;
}
