package com.kittyp.notification.entity;

import com.kittyp.common.entity.BaseEntity;
import com.kittyp.notification.enums.MessageTenantType;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity @Table(name="messaging_audit_events", indexes=@Index(name="idx_message_audit_tenant_created",columnList="tenant_type,tenant_id,created_at"))
@Data @EqualsAndHashCode(callSuper=true)
public class MessageAuditEvent extends BaseEntity {
    @Enumerated(EnumType.STRING) @Column(name="tenant_type",nullable=false,length=16) private MessageTenantType tenantType;
    @Column(name="tenant_id",nullable=false) private Long tenantId;
    @Column(name="message_uuid",length=36) private String messageUuid;
    @Column(name="actor_email",length=240) private String actorEmail;
    @Column(nullable=false,length=32) private String action;
    @Column(name="safe_detail",length=300) private String safeDetail;
}
