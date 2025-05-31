package com.kittyp.cart.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kittyp.cart.entity.CartItem;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    void deleteByCartUuidAndProductUuid(String cartUuid, String productUuid);
    
}

