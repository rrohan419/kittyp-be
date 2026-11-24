package com.kittyp.notification.repository;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.kittyp.notification.entity.MessageAuditEvent;
import com.kittyp.notification.enums.MessageTenantType;
public interface MessageAuditRepository extends JpaRepository<MessageAuditEvent,Long> {
    List<MessageAuditEvent> findTop100ByTenantTypeAndTenantIdOrderByCreatedAtDesc(MessageTenantType type,Long tenantId);
}
