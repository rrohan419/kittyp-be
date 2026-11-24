package com.kittyp.notification.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import com.kittyp.notification.entity.PracticeMessage;
import com.kittyp.notification.enums.MessageStatus;
import com.kittyp.notification.enums.MessageTenantType;

public interface PracticeMessageRepository extends JpaRepository<PracticeMessage, Long> {
    Optional<PracticeMessage> findByUuidAndTenantTypeAndTenantId(String uuid, MessageTenantType type, Long tenantId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from PracticeMessage m where m.uuid=:uuid and m.tenantType=:type and m.tenantId=:tenantId")
    Optional<PracticeMessage> lockByUuidAndTenant(@Param("uuid") String uuid,
            @Param("type") MessageTenantType type,@Param("tenantId") Long tenantId);
    List<PracticeMessage> findTop200ByTenantTypeAndTenantIdOrderByScheduledAtDesc(MessageTenantType type, Long tenantId);
    List<PracticeMessage> findTop200ByTenantTypeAndTenantIdAndStatusInOrderByScheduledAtAsc(
            MessageTenantType type, Long tenantId, List<MessageStatus> statuses);
    long countByTenantTypeAndTenantIdAndStatus(MessageTenantType type, Long tenantId, MessageStatus status);
    long countByTenantTypeAndTenantIdAndChannelAndCreatedAtGreaterThanEqualAndStatusIn(
            MessageTenantType type, Long tenantId, com.kittyp.notification.enums.MessageChannel channel,
            LocalDateTime from, List<MessageStatus> statuses);
    Optional<PracticeMessage> findByTenantTypeAndTenantIdAndIdempotencyKey(
            MessageTenantType type, Long tenantId, String idempotencyKey);
    Optional<PracticeMessage> findByProviderMessageId(String providerMessageId);
    Optional<PracticeMessage> findByTenantTypeAndTenantIdAndSourceTypeAndSourceUuidAndAutomationId(
            MessageTenantType tenantType, Long tenantId, String sourceType, String sourceUuid, String automationId);
    List<PracticeMessage> findByTenantTypeAndTenantIdAndSourceTypeAndSourceUuid(
            MessageTenantType tenantType, Long tenantId, String sourceType, String sourceUuid);
    List<PracticeMessage> findByTenantTypeAndTenantIdAndAutomationId(
            MessageTenantType tenantType, Long tenantId, String automationId);

    @Modifying(clearAutomatically=true, flushAutomatically=true)
    @Query("""
        update PracticeMessage m set m.status = com.kittyp.notification.enums.MessageStatus.SENDING,
            m.attempts = m.attempts + 1
        where m.id = :id and m.status in
            (com.kittyp.notification.enums.MessageStatus.QUEUED, com.kittyp.notification.enums.MessageStatus.SCHEDULED)
            and m.scheduledAt <= :now
            and m.automationId is null
            and not exists (select s.id from MessageSettings s
                where s.tenantType = m.tenantType and s.tenantId = m.tenantId and s.paused = true)
        """)
    int claim(@Param("id") Long id, @Param("now") LocalDateTime now);

    List<PracticeMessage> findTop100ByAutomationIdIsNullAndStatusInAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
            List<MessageStatus> statuses, LocalDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update PracticeMessage m
        set m.status = com.kittyp.notification.enums.MessageStatus.CANCELLED
        where m.automationId in ('VACCINE_REMINDER', 'PET_REMINDER')
          and m.status in (
            com.kittyp.notification.enums.MessageStatus.QUEUED,
            com.kittyp.notification.enums.MessageStatus.SCHEDULED,
            com.kittyp.notification.enums.MessageStatus.PAUSED,
            com.kittyp.notification.enums.MessageStatus.FAILED
          )
          and m.scheduledAt < :cutoff
        """)
    int cancelExpiredReminderAutomations(@Param("cutoff") LocalDateTime cutoff);
}
