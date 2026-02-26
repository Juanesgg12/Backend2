package co.edu.cesde.pps.service;

import co.edu.cesde.pps.dto.CartDTO;
import co.edu.cesde.pps.enums.CartStatus;
import co.edu.cesde.pps.exception.CartMergeException;
import co.edu.cesde.pps.exception.EntityNotFoundException;
import co.edu.cesde.pps.exception.InsufficientStockException;
import co.edu.cesde.pps.exception.InvalidCartStateException;
import co.edu.cesde.pps.exception.ValidationException;
import co.edu.cesde.pps.mapper.CartMapper;
import co.edu.cesde.pps.model.*;
import co.edu.cesde.pps.util.CalculationUtils;
import co.edu.cesde.pps.util.ValidationUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CartService {

    private final CartMapper cartMapper;
    private final UserService userService;
    private final ProductService productService;
    private final List<Cart> cartsInMemory;

    public CartService(UserService userService, ProductService productService) {
        this.cartMapper = new CartMapper();
        this.userService = userService;
        this.productService = productService;
        this.cartsInMemory = new ArrayList<>();
    }

    public CartDTO createCartForGuest(Long sessionId) {

        Cart cart = Cart.builder()
                .cartId(generateNextId())
                .user(null)
                .status(CartStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .items(new ArrayList<>())
                .build();

        cartsInMemory.add(cart);
        return cartMapper.toDTO(cart);
    }

    public CartDTO createCartForUser(Long userId) {

        User user = userService.findUserEntityOrThrow(userId);

        Cart cart = Cart.builder()
                .cartId(generateNextId())
                .user(user)
                .status(CartStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .items(new ArrayList<>())
                .build();

        cartsInMemory.add(cart);
        return cartMapper.toDTO(cart);
    }

    public CartDTO findById(Long cartId) {
        return cartMapper.toDTO(findCartEntityOrThrow(cartId));
    }

    public CartDTO addItem(Long cartId, Long productId, Integer quantity) {

        ValidationUtils.validatePositive(quantity, "quantity");

        Cart cart = findCartEntityOrThrow(cartId);

        if (cart.getStatus() != CartStatus.OPEN) {
            throw new InvalidCartStateException(cartId, cart.getStatus(),
                    CartStatus.OPEN, "add item");
        }

        Product product = productService.findProductEntityOrThrow(productId);

        if (!product.getIsActive()) {
            throw new ValidationException("Product '" + product.getName() + "' is not active");
        }

        if (!CalculationUtils.hasEnoughStock(product.getStockQty(), quantity)) {
            throw new InsufficientStockException(productId, product.getSku(),
                    quantity, product.getStockQty());
        }

        CartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getProductId().equals(productId))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {

            int newQuantity = existingItem.getQuantity() + quantity;

            if (!CalculationUtils.hasEnoughStock(product.getStockQty(), newQuantity)) {
                throw new InsufficientStockException(productId, product.getSku(),
                        newQuantity, product.getStockQty());
            }

            existingItem.setQuantity(newQuantity);

        } else {

            CartItem newItem = CartItem.builder()
                    .cartItemId(generateNextCartItemId())
                    .cart(cart)
                    .product(product)
                    .quantity(quantity)
                    .unitPrice(product.getPrice())
                    .addedAt(LocalDateTime.now())
                    .build();

            cart.getItems().add(newItem);
        }

        touchCart(cart);
        return cartMapper.toDTO(cart);
    }

    public CartDTO mergeGuestCartToUserCart(Long guestCartId, Long userId) {

        Cart guestCart = findCartEntityOrThrow(guestCartId);
        Cart userCart = findOrCreateOpenCartForUser(userId);

        if (guestCart.getStatus() != CartStatus.OPEN) {
            throw new InvalidCartStateException(guestCartId,
                    guestCart.getStatus(), CartStatus.OPEN, "merge");
        }

        if (userCart.getStatus() != CartStatus.OPEN) {
            throw new InvalidCartStateException(userCart.getCartId(),
                    userCart.getStatus(), CartStatus.OPEN, "merge");
        }

        if (guestCart.getUser() != null) {
            throw new CartMergeException(guestCartId, userCart.getCartId(),
                    "Guest cart already has a user assigned");
        }

        for (CartItem guestItem : new ArrayList<>(guestCart.getItems())) {

            Product product = guestItem.getProduct();
            Integer guestQuantity = guestItem.getQuantity();

            CartItem userItem = userCart.getItems().stream()
                    .filter(item -> item.getProduct().getProductId()
                            .equals(product.getProductId()))
                    .findFirst()
                    .orElse(null);

            if (userItem != null) {

                int totalQuantity = userItem.getQuantity() + guestQuantity;

                if (!CalculationUtils.hasEnoughStock(product.getStockQty(), totalQuantity)) {
                    throw new InsufficientStockException(product.getProductId(),
                            product.getSku(), totalQuantity, product.getStockQty());
                }

                userItem.setQuantity(totalQuantity);

                if (guestItem.getAddedAt().isAfter(userItem.getAddedAt())) {
                    userItem.setUnitPrice(guestItem.getUnitPrice());
                }

            } else {

                if (!CalculationUtils.hasEnoughStock(product.getStockQty(), guestQuantity)) {
                    throw new InsufficientStockException(product.getProductId(),
                            product.getSku(), guestQuantity, product.getStockQty());
                }

                CartItem newItem = CartItem.builder()
                        .cartItemId(generateNextCartItemId())
                        .cart(userCart)
                        .product(product)
                        .quantity(guestQuantity)
                        .unitPrice(guestItem.getUnitPrice())
                        .addedAt(guestItem.getAddedAt())
                        .build();

                userCart.getItems().add(newItem);
            }
        }

        guestCart.setStatus(CartStatus.ABANDONED);
        touchCart(guestCart);
        touchCart(userCart);

        return cartMapper.toDTO(userCart);
    }

    public BigDecimal calculateCartTotal(Long cartId) {
        Cart cart = findCartEntityOrThrow(cartId);
        return cart.calculateTotal();
    }

    public Cart findCartEntityOrThrow(Long cartId) {
        return cartsInMemory.stream()
                .filter(c -> c.getCartId().equals(cartId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Cart", cartId));
    }

    private Cart findOrCreateOpenCartForUser(Long userId) {

        User user = userService.findUserEntityOrThrow(userId);

        Cart cart = cartsInMemory.stream()
                .filter(c -> c.getUser() != null &&
                        c.getUser().getUserId().equals(userId) &&
                        c.getStatus() == CartStatus.OPEN)
                .findFirst()
                .orElse(null);

        if (cart == null) {

            cart = Cart.builder()
                    .cartId(generateNextId())
                    .user(user)
                    .status(CartStatus.OPEN)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .items(new ArrayList<>())
                    .build();

            cartsInMemory.add(cart);
        }

        return cart;
    }

    private void touchCart(Cart cart) {
        cart.setUpdatedAt(LocalDateTime.now());
    }

    private Long generateNextId() {
        return cartsInMemory.stream()
                .mapToLong(Cart::getCartId)
                .max()
                .orElse(0L) + 1;
    }

    private Long generateNextCartItemId() {
        return cartsInMemory.stream()
                .flatMap(cart -> cart.getItems().stream())
                .mapToLong(CartItem::getCartItemId)
                .max()
                .orElse(0L) + 1;
    }
}
