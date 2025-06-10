package com.kittyp.product.entity;

import java.time.LocalDateTime;

import com.kittyp.common.entity.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stock_reservations")
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockReservation extends BaseEntity {
    
    @ManyToOne
    private Product product;
    
    @Column(nullable = false)
    private Integer quantity;
    
    @Column(nullable = false)
    private String orderNumber;
    
    @Column(nullable = false)
    private LocalDateTime expirationTime;
    
    @Column(nullable = false)
    private boolean confirmed;
} 