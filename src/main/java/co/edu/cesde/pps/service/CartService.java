package co.edu.cesde.pps.service;

import co.edu.cesde.pps.dto.CartDTO;
import co.edu.cesde.pps.enums.CartStatus;
import co.edu.cesde.pps.exception.*;
import co.edu.cesde.pps.mapper.CartMapper;
import co.edu.cesde.pps.model.*;
import co.edu.cesde.pps.repository.CartRepository;
import co.edu.cesde.pps.repository.UserSesionRepository;
import co.edu.cesde.pps.util.CalculationUtils;
import co.edu.cesde.pps.util.ValidationUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CartService {

    private final CartMapper cartMapper;
    private final CartRepository cartRepository;
    private final UserSesionRepository userSesionRepository;
    private final UserService userService;
    private final ProductService productService;

    public CartService(CartRepository cartRepository,
                       UserSesionRepository userSesionRepository,
                       UserService userService,
                       ProductService productService) {
        this.cartMapper = new CartMapper();
        this.cartRepository = cartRepository;
        this.userSesionRepository = userSesionRepository;
        this.userService = userService;
        this.productService = productService;
    }

    // Crea un carrito para un invitado dado su sessionId (ya persistido en BD)
    @Transactional
    public CartDTO createCartForGuest(Long sessionId) {
        // La sesión debe existir en BD antes de crear el carrito
        UserSession session = userSesionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("UserSession", sessionId));

        Cart cart = Cart.builder()
                .user(null)          // null = carrito de invitado
                .session(session)
                .status(CartStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .items(new ArrayList<>())
                .build();

        Cart saved = cartRepository.save(cart);
        return cartMapper.toDTO(saved);
    }

    @Transactional
    public CartDTO createCartForUser(Long userId) {
        User user = userService.findUserEntityOrThrow(userId);

        // Reutilizar la sesión del usuario si tiene una activa,
        // o crear una nueva. Por simplicidad, usamos la primera sesión del usuario.
        // En etapa12 esto se gestionará desde AuthController con UserSession real.
        Cart cart = Cart.builder()
                .user(user)
                .status(CartStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .items(new ArrayList<>())
                .build();

        Cart saved = cartRepository.save(cart);
        return cartMapper.toDTO(saved);
    }

    public CartDTO findById(Long cartId) {
        return cartMapper.toDTO(findCartEntityOrThrow(cartId));
    }

    @Transactional
    public CartDTO addItem(Long cartId, Long productId, Integer quantity) {
        ValidationUtils.validatePositive(quantity, "quantity");

        Cart cart = findCartEntityOrThrow(cartId);

        if (cart.getStatus() != CartStatus.OPEN) {
            throw new InvalidCartStateException(
                    cartId, cart.getStatus(), CartStatus.OPEN, "add item");
        }

        Product product = productService.findProductEntityOrThrow(productId);

        if (!product.getIsActive()) {
            throw new ValidationException(
                    "Product '" + product.getName() + "' is not active");
        }

        if (!CalculationUtils.hasEnoughStock(product.getStockQty(), quantity)) {
            throw new InsufficientStockException(
                    productId, product.getSku(), quantity, product.getStockQty());
        }

        // Buscar si el producto ya está en el carrito
        CartItem existingItem = cart.getItems().stream()
                .filter(i -> i.getProduct().getProductId().equals(productId))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            int newQuantity = existingItem.getQuantity() + quantity;
            if (!CalculationUtils.hasEnoughStock(product.getStockQty(), newQuantity)) {
                throw new InsufficientStockException(
                        productId, product.getSku(), newQuantity, product.getStockQty());
            }
            existingItem.setQuantity(newQuantity);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(quantity)
                    .unitPrice(product.getPrice())
                    .addedAt(LocalDateTime.now())
                    .build();
            // Al agregar a la colección con CascadeType.ALL,
            // JPA persiste el CartItem automáticamente al guardar el Cart
            cart.getItems().add(newItem);
        }

        cart.setUpdatedAt(LocalDateTime.now());
        Cart saved = cartRepository.save(cart);
        return cartMapper.toDTO(saved);
    }

    @Transactional
    public CartDTO updateItemQuantity(Long cartId, Long productId, Integer quantity) {
        ValidationUtils.validatePositive(quantity, "quantity");

        Cart cart = findCartEntityOrThrow(cartId);

        if (cart.getStatus() != CartStatus.OPEN) {
            throw new InvalidCartStateException(
                    cartId, cart.getStatus(), CartStatus.OPEN, "update item");
        }

        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProduct().getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(
                        "CartItem for product " + productId));

        Product product = item.getProduct();
        if (!CalculationUtils.hasEnoughStock(product.getStockQty(), quantity)) {
            throw new InsufficientStockException(
                    productId, product.getSku(), quantity, product.getStockQty());
        }

        item.setQuantity(quantity);
        cart.setUpdatedAt(LocalDateTime.now());
        Cart saved = cartRepository.save(cart);
        return cartMapper.toDTO(saved);
    }

    @Transactional
    public CartDTO removeItem(Long cartId, Long productId) {
        Cart cart = findCartEntityOrThrow(cartId);

        if (cart.getStatus() != CartStatus.OPEN) {
            throw new InvalidCartStateException(
                    cartId, cart.getStatus(), CartStatus.OPEN, "remove item");
        }

        // removeIf con orphanRemoval=true en Cart elimina el CartItem de la BD
        boolean removed = cart.getItems().removeIf(
                i -> i.getProduct().getProductId().equals(productId));

        if (!removed) {
            throw new EntityNotFoundException("CartItem for product " + productId);
        }

        cart.setUpdatedAt(LocalDateTime.now());
        Cart saved = cartRepository.save(cart);
        return cartMapper.toDTO(saved);
    }

    @Transactional
    public CartDTO clearCart(Long cartId) {
        Cart cart = findCartEntityOrThrow(cartId);

        if (cart.getStatus() != CartStatus.OPEN) {
            throw new InvalidCartStateException(
                    cartId, cart.getStatus(), CartStatus.OPEN, "clear");
        }

        // clear() + orphanRemoval=true → JPA borra todos los CartItems en BD
        cart.getItems().clear();
        cart.setUpdatedAt(LocalDateTime.now());
        Cart saved = cartRepository.save(cart);
        return cartMapper.toDTO(saved);
    }

    @Transactional
    public CartDTO mergeGuestCartToUserCart(Long guestCartId, Long userId) {
        Cart guestCart = findCartEntityOrThrow(guestCartId);
        Cart userCart = findOrCreateOpenCartForUser(userId);

        if (guestCart.getStatus() != CartStatus.OPEN) {
            throw new InvalidCartStateException(
                    guestCartId, guestCart.getStatus(), CartStatus.OPEN, "merge");
        }
        if (guestCart.getUser() != null) {
            throw new CartMergeException(guestCartId, userCart.getCartId(),
                    "Guest cart already has a user assigned");
        }

        for (CartItem guestItem : new ArrayList<>(guestCart.getItems())) {
            Product product = guestItem.getProduct();
            Integer guestQty = guestItem.getQuantity();

            CartItem userItem = userCart.getItems().stream()
                    .filter(i -> i.getProduct().getProductId()
                            .equals(product.getProductId()))
                    .findFirst()
                    .orElse(null);

            if (userItem != null) {
                int totalQty = userItem.getQuantity() + guestQty;
                if (!CalculationUtils.hasEnoughStock(product.getStockQty(), totalQty)) {
                    throw new InsufficientStockException(
                            product.getProductId(), product.getSku(),
                            totalQty, product.getStockQty());
                }
                userItem.setQuantity(totalQty);
                if (guestItem.getAddedAt().isAfter(userItem.getAddedAt())) {
                    userItem.setUnitPrice(guestItem.getUnitPrice());
                }
            } else {
                CartItem newItem = CartItem.builder()
                        .cart(userCart)
                        .product(product)
                        .quantity(guestQty)
                        .unitPrice(guestItem.getUnitPrice())
                        .addedAt(guestItem.getAddedAt())
                        .build();
                userCart.getItems().add(newItem);
            }
        }

        guestCart.setStatus(CartStatus.ABANDONED);
        guestCart.setUpdatedAt(LocalDateTime.now());
        userCart.setUpdatedAt(LocalDateTime.now());

        cartRepository.save(guestCart);
        Cart saved = cartRepository.save(userCart);
        return cartMapper.toDTO(saved);
    }

    public BigDecimal calculateCartTotal(Long cartId) {
        return findCartEntityOrThrow(cartId).calculateTotal();
    }

    public Cart findCartEntityOrThrow(Long cartId) {
        return cartRepository.findById(cartId)
                .orElseThrow(() -> new EntityNotFoundException("Cart", cartId));
    }

    // Busca el carrito abierto del usuario, o crea uno nuevo si no tiene
    private Cart findOrCreateOpenCartForUser(Long userId) {
        return cartRepository
                .findByUserUserIdAndStatus(userId, CartStatus.OPEN)
                .orElseGet(() -> {
                    User user = userService.findUserEntityOrThrow(userId);
                    Cart newCart = Cart.builder()
                            .user(user)
                            .status(CartStatus.OPEN)
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .items(new ArrayList<>())
                            .build();
                    return cartRepository.save(newCart);
                });
    }
}