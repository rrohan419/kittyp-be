package com.kittyp.cart.service;

import com.kittyp.cart.dto.AddToCartRequest;
import com.kittyp.cart.dto.CartCheckoutRequest;
import com.kittyp.cart.dto.CartItemRequest;
import com.kittyp.cart.entity.Cart;
import com.kittyp.cart.model.CartResponse;
import com.kittyp.order.model.OrderModel;
import com.kittyp.user.entity.User;

public interface CartService {
    CartResponse getCartByUser(String userUuid);
    
    CartResponse addToCart(String userUuid, CartItemRequest request);
    
    CartResponse updateCartItem(String userUuid, CartItemRequest request);
    
    CartResponse removeFromCart(String userUuid, String productUuid);
    
    void clearCart(String userUuid);
    
    Cart getOrCreateCart(User user);
//    /**
//     * Creates an order from the cart with shipping and billing details
//     * @param userUuid User's UUID
//     * @param request Checkout details including addresses
//     * @return Created order details
//     */
//    OrderModel createOrderFromCart(String userUuid, CartCheckoutRequest request);
}

