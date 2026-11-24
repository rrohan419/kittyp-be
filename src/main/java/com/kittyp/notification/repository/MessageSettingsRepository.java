package com.kittyp.notification.repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.kittyp.notification.entity.MessageSettings;
import com.kittyp.notification.enums.MessageTenantType;
public interface MessageSettingsRepository extends JpaRepository<MessageSettings,Long> {
    Optional<MessageSettings> findByTenantTypeAndTenantId(MessageTenantType type,Long tenantId);
}
