package com.kittyp.notification.repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.kittyp.notification.entity.MessageRateCard;
import com.kittyp.notification.enums.MessageChannel;
public interface MessageRateCardRepository extends JpaRepository<MessageRateCard,Long> {
    Optional<MessageRateCard> findByChannel(MessageChannel channel);
}
