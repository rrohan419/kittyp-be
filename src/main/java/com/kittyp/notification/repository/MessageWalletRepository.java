package com.kittyp.notification.repository;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import com.kittyp.notification.entity.MessageWallet;
import com.kittyp.notification.enums.MessageChannel;
import com.kittyp.notification.enums.MessageTenantType;
public interface MessageWalletRepository extends JpaRepository<MessageWallet,Long> {
    Optional<MessageWallet> findByTenantTypeAndTenantIdAndChannel(MessageTenantType type,Long tenantId,MessageChannel channel);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from MessageWallet w where w.tenantType=:type and w.tenantId=:tenantId and w.channel=:channel")
    Optional<MessageWallet> lockWallet(@Param("type") MessageTenantType type,@Param("tenantId") Long tenantId,@Param("channel") MessageChannel channel);
}
