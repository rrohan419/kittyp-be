package com.kittyp.notification.repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import com.kittyp.notification.entity.MessagePurchase;
public interface MessagePurchaseRepository extends JpaRepository<MessagePurchase,Long> {
    Optional<MessagePurchase> findByRazorpayOrderId(String orderId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from MessagePurchase p where p.razorpayOrderId=:orderId")
    Optional<MessagePurchase> lockByRazorpayOrderId(@Param("orderId") String orderId);
    Optional<MessagePurchase> findByRazorpayPaymentId(String paymentId);
}
