package com.kittyp.notification.entity;

import java.time.LocalDateTime;

import com.kittyp.common.entity.BaseEntity;
import com.kittyp.common.entity.HasPublicId;
import com.kittyp.common.entity.PublicIdEntityListener;
import com.kittyp.notification.enums.MessageChannel;
import com.kittyp.notification.enums.MessageStatus;
import com.kittyp.notification.enums.MessageTenantType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "practice_messages", indexes = {
                @Index(name = "idx_practice_message_due", columnList = "status,scheduled_at"),
                @Index(name = "idx_practice_message_tenant", columnList = "tenant_type,tenant_id,created_at")
}, uniqueConstraints = @UniqueConstraint(name = "uk_practice_message_tenant_idem", columnNames = { "tenant_type",
                "tenant_id", "idempotency_key" }))
@EntityListeners(PublicIdEntityListener.class)
@Data
@EqualsAndHashCode(callSuper = true)
public class PracticeMessage extends BaseEntity implements HasPublicId {
        @Column(nullable = false, unique = true, updatable = false, length = 36)
        private String uuid;
        @Enumerated(EnumType.STRING)
        @Column(name = "tenant_type", nullable = false, length = 16)
        private MessageTenantType tenantType;
        @Column(name = "tenant_id", nullable = false)
        private Long tenantId;
        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 16)
        private MessageChannel channel;
        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 16)
        private MessageStatus status;
        @Column(nullable = false, columnDefinition = "TEXT")
        private String recipient;
        @Column(name = "recipient_name", length = 160)
        private String recipientName;
        @Column(name = "pet_name", length = 160)
        private String petName;
        @Column(length = 200)
        private String subject;
        @Column(name = "template_key", length = 120)
        private String templateKey;
        @Column(name = "template_variables", columnDefinition = "TEXT")
        private String templateVariables;
        @Column(name = "email_body", columnDefinition = "TEXT")
        private String emailBody;
        @Column(name = "message_body_snapshot", columnDefinition = "TEXT")
        private String messageBodySnapshot;
        @Column(name = "scheduled_at", nullable = false)
        private LocalDateTime scheduledAt;
        @Column(name = "sent_at")
        private LocalDateTime sentAt;
        @Column(name = "idempotency_key", length = 100)
        private String idempotencyKey;
        @Column(name = "provider_message_id", length = 160)
        private String providerMessageId;
        @Column(name = "failure_code", length = 80)
        private String failureCode;
        @Column(nullable = false)
        private Integer attempts = 0;
        @Column(name = "reserved_credits", nullable = false)
        private Integer reservedCredits = 0;
        @Column(name = "free_allowance_reserved", nullable = false)
        private Boolean freeAllowanceReserved = false;
        @Column(name = "automation_id", length = 100)
        private String automationId;
        @Column(name = "source_type", length = 100)
        private String sourceType;
        @Column(name = "source_uuid", length = 100)
        private String sourceUuid;
}
