package com.kittyp.notification.entity;

import com.kittyp.common.entity.BaseEntity;
import com.kittyp.notification.enums.MessageTenantType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "messaging_settings", uniqueConstraints = @UniqueConstraint(name = "uk_messaging_settings_tenant", columnNames = {
        "tenant_type", "tenant_id" }))
@Data
@EqualsAndHashCode(callSuper = true)
public class MessageSettings extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(name = "tenant_type", nullable = false, length = 16)
    private MessageTenantType tenantType;
    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;
    @Column(name = "paused", nullable = false)
    private Boolean paused = false;
    @Column(name = "automations_json", columnDefinition = "TEXT")
    private String automationsJson = "{}";
}
