package com.kittyp.cart.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;

import com.kittyp.cart.dto.CartItemRequest;
import com.kittyp.cart.entity.Cart;
import com.kittyp.cart.entity.CartItem;
import com.kittyp.cart.model.CartItemResponse;
import com.kittyp.cart.model.CartResponse;
import com.kittyp.cart.repository.CartRepository;
import com.kittyp.common.exception.CustomException;
import com.kittyp.product.dao.ProductDao;
import com.kittyp.product.entity.Product;
import com.kittyp.user.dao.UserDao;
import com.kittyp.user.entity.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final ProductDao productDao;
    private final UserDao userDao;

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public CartResponse getCartByUser(String userUuid) {
        User user = userDao.userByUuid(userUuid);
        if (user == null) {
            throw new CustomException("User not found", HttpStatus.NOT_FOUND);
        }

        Cart cart = getOrCreateCart(user);
        // No need to force initialization as we ensure list is never null

        return convertToCartModel(cart);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public CartResponse addToCart(String userUuid, CartItemRequest request) {
        User user = userDao.userByUuid(userUuid);
        Product product = productDao.productUuid(request.getProductUuid());

        if (product == null) {
            throw new CustomException("Product not found", HttpStatus.NOT_FOUND);
        }

        Cart cart = getOrCreateCart(user);

        // Check if product already exists in cart
        Optional<CartItem> existingItem = cart.getCartItems().stream()
                .filter(item -> item.getProduct().getUuid().equals(request.getProductUuid()))
                .findFirst();

        if (existingItem.isPresent()) {
            // Update quantity
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            item.setTotal(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        } else {
            // Add new item
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.getQuantity())
                    .price(product.getPrice())
                    .total(product.getPrice().multiply(BigDecimal.valueOf(request.getQuantity())))
                    .build();
            cart.addCartItem(newItem);
        }

        cart = cartRepository.save(cart);

        return convertToCartModel(cart);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public CartResponse updateCartItem(String userUuid, CartItemRequest request) {
        User user = userDao.userByUuid(userUuid);
        Cart cart = getOrCreateCart(user);

        CartItem item = cart.getCartItems().stream()
                .filter(i -> i.getProduct().getUuid().equals(request.getProductUuid()))
                .findFirst()
                .orElseThrow(() -> new CustomException("Product not found in cart", HttpStatus.NOT_FOUND));

        if (request.getQuantity() <= 0) {
            cart.removeCartItem(item);
        } else {
            item.setQuantity(request.getQuantity());
            item.setTotal(item.getPrice().multiply(BigDecimal.valueOf(request.getQuantity())));
        }

        cart = cartRepository.save(cart);
        return convertToCartModel(cart);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public CartResponse removeFromCart(String userUuid, String productUuid) {
        User user = userDao.userByUuid(userUuid);
        Cart cart = getOrCreateCart(user);

        CartItem item = cart.getCartItems().stream()
                .filter(i -> i.getProduct().getUuid().equals(productUuid))
                .findFirst()
                .orElseThrow(() -> new CustomException("Product not found in cart", HttpStatus.NOT_FOUND));

        cart.removeCartItem(item);
        cart = cartRepository.save(cart);
        return convertToCartModel(cart);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public void clearCart(String userUuid) {
        User user = userDao.userByUuid(userUuid);
        Cart cart = getOrCreateCart(user);
        cart.getCartItems().clear();
        cartRepository.save(cart);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, isolation = Isolation.READ_COMMITTED)
    public Cart getOrCreateCart(User user) {
        return cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    newCart.setUuid(UUID.randomUUID().toString());
                    newCart.setCartItems(new ArrayList<>());
                    newCart.setIsActive(true); // Set active status
                    return cartRepository.save(newCart);
                });
    }

    private CartResponse convertToCartModel(Cart cart) {
        List<CartItemResponse> cartItemResponses = null;

        if (cart.getCartItems() != null) {
            cartItemResponses = cart.getCartItems().stream()
                    .sorted(Comparator.comparing(CartItem::getCreatedAt,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .map(item -> CartItemResponse.builder()
                            .productUuid(item.getProduct().getUuid())
                            .productName(item.getProduct().getName())
                            .price(item.getPrice())
                            .quantity(item.getQuantity())
                            .totalPrice(item.getTotal())
                            .build())
                    .toList();
        }

        BigDecimal totalAmount = cartItemResponses != null ? cartItemResponses.stream()
                .map(CartItemResponse::getTotalPrice)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;

        return CartResponse.builder()
                .uuid(cart.getUuid())
                .items(cartItemResponses)
                .totalAmount(totalAmount)
                .build();
    }
}
