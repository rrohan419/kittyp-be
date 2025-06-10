package com.kittyp.product.dao;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.kittyp.product.entity.StockReservation;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {
    
    List<StockReservation> findByOrderNumber(String orderNumber);
    
    @Query("SELECT COALESCE(SUM(sr.quantity), 0) FROM StockReservation sr " +
           "WHERE sr.product.id = :productId " +
           "AND sr.confirmed = false " +
           "AND sr.expirationTime > :now")
    int findActiveReservationsQuantityForProduct(@Param("productId") Long productId, @Param("now") LocalDateTime now);
    
    List<StockReservation> findByExpirationTimeBefore(LocalDateTime time);
} 