package com.kittyp.notification.repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.kittyp.notification.entity.MessageLedgerEntry;
import com.kittyp.notification.enums.MessageChannel;
import com.kittyp.notification.enums.MessageTenantType;
public interface MessageLedgerRepository extends JpaRepository<MessageLedgerEntry,Long> {
    List<MessageLedgerEntry> findTop100ByTenantTypeAndTenantIdOrderByCreatedAtDesc(MessageTenantType type,Long tenantId);
    Optional<MessageLedgerEntry> findByTenantTypeAndTenantIdAndChannelAndIdempotencyKey(
        MessageTenantType type,Long tenantId,MessageChannel channel,String idempotencyKey);
}
