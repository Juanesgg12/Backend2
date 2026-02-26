package co.edu.cesde.pps.service;

import co.edu.cesde.pps.dto.CartDTO;
import co.edu.cesde.pps.enums.CartStatus;
import co.edu.cesde.pps.exception.CartMergeException;
import co.edu.cesde.pps.exception.EntityNotFoundException;
import co.edu.cesde.pps.exception.InsufficientStockException;
import co.edu.cesde.pps.exception.InvalidCartStateException;
import co.edu.cesde.pps.exception.ValidationException;
import co.edu.cesde.pps.mapper.CartMapper;
import co.edu.cesde.pps.model.Cart;
import co.edu.cesde.pps.model.CartItem;
import co.edu.cesde.pps.model.Product;
import co.edu.cesde.pps.model.User;
import co.edu.cesde.pps.util.CalculationUtils;
import co.edu.cesde.pps.util.ValidationUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para gestión de carritos de compra.
 *
 * Responsabilidades:
 * - CRUD de carritos
 * - Agregar/actualizar/remover items (gestión bidireccional)
 * - Calcular totales
 * - Validar disponibilidad de productos
 * - Actualizar timestamp (touch)
 * - **ALGORITMO DE CART MERGE** (fusión invitado → registrado)
 * - Limpiar carrito
 * - Conversión Entity <-> DTO
 *
 * NOTA: En Etapa 06 se agregará:
 * - @Service annotation
 * - @Transactional (crítico para Cart Merge)
 * - Inyección de CartRepository
 * - Persistencia real
 */
public class CartService {

    private final CartMapper cartMapper;
    private final UserService userService;
    private final ProductService productService;
    // TODO Etapa 06: private final CartRepository cartRepository;
    private final List<Cart> cartsInMemory;

    public CartService(UserService userService, ProductService productService) {
        this.cartMapper = new CartMapper();
        this.userService = userService;
        this.productService = productService;
        this.cartsInMemory = new ArrayList<>();
    }

    /**
     * Crea un carrito para invitado (sin usuario).
     *
     * @param sessionId ID de la sesión
     * @return CartDTO del carrito creado
     */
    public CartDTO createCartForGuest(Long sessionId) {
        Cart cart = new Cart();
        cart.setCartId(generateNextId());
        cart.setUser(null); // Invitado
        cart.setStatus(CartStatus.OPEN);
        cart.setCreatedAt(LocalDateTime.now());
        cart.setUpdatedAt(LocalDateTime.now());

        // TODO Etapa 06: cartRepository.save(cart);
        cartsInMemory.add(cart);

        return cartMapper.toDTO(cart);
    }

    /**
     * Crea un carrito para usuario registrado.
     *
     * @param userId ID del usuario
     * @return CartDTO del carrito creado
     * @throws EntityNotFoundException si el usuario no existe
     */
    public CartDTO createCartForUser(Long userId) {
        User user = userService.findUserEntityOrThrow(userId);

        Cart cart = new Cart();
        cart.setCartId(generateNextId());
        cart.setUser(user);
        cart.setStatus(CartStatus.OPEN);
        cart.setCreatedAt(LocalDateTime.now());
        cart.setUpdatedAt(LocalDateTime.now());

        // TODO Etapa 06: cartRepository.save(cart);
        cartsInMemory.add(cart);

        return cartMapper.toDTO(cart);
    }

    /**
     * Busca carrito por ID.
     *
     * @param cartId ID del carrito
     * @return CartDTO
     * @throws EntityNotFoundException si no existe
     */
    public CartDTO findById(Long cartId) {
        Cart cart = findCartEntityOrThrow(cartId);
        return cartMapper.toDTO(cart);
    }

    /**
     * Busca carrito OPEN del usuario (puede no existir).
     *
     * @param userId ID del usuario
     * @return CartDTO o null si no existe
     */
    public CartDTO findOpenCartByUser(Long userId) {
        // TODO Etapa 06: Optional<Cart> cart = cartRepository.findByUserIdAndStatus(userId, CartStatus.OPEN);
        Cart cart = cartsInMemory.stream()
                .filter(c -> c.getUser() != null &&
                        c.getUser().getUserId().equals(userId) &&
                        c.getStatus() == CartStatus.OPEN)
                .findFirst()
                .orElse(null);

        return cart != null ? cartMapper.toDTO(cart) : null;
    }

    /**
     * Agrega un item al carrito (gestión bidireccional).
     *
     * @param cartId ID del carrito
     * @param productId ID del producto
     * @param quantity Cantidad a agregar
     * @return CartDTO actualizado
     * @throws EntityNotFoundException si carrito o producto no existen
     * @throws InvalidCartStateException si el carrito no está OPEN
     * @throws InsufficientStockException si no hay stock suficiente
     * @throws ValidationException si el producto no está activo
     */
    public CartDTO addItem(Long cartId, Long productId, Integer quantity) {
        // Validar cantidad
        ValidationUtils.validatePositive(quantity, "quantity");

        // Obtener carrito y validar estado
        Cart cart = findCartEntityOrThrow(cartId);
        if (cart.getStatus() != CartStatus.OPEN) {
            throw new InvalidCartStateException(cartId, cart.getStatus(),
                    CartStatus.OPEN, "add item");
        }

        // Obtener producto y validar disponibilidad
        Product product = productService.findProductEntityOrThrow(productId);
        if (!product.getIsActive()) {
            throw new ValidationException("Product '" + product.getName() + "' is not active");
        }

        // Validar stock disponible
        if (!CalculationUtils.hasEnoughStock(product.getStockQty(), quantity)) {
            throw new InsufficientStockException(productId, product.getSku(),
                    quantity, product.getStockQty());
        }

        // Buscar si el producto ya existe en el carrito
        CartItem existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getProductId().equals(productId))
                .findFirst()
                .orElse(null);

        if (existingItem != null) {
            // Producto ya existe: actualizar cantidad
            int newQuantity = existingItem.getQuantity() + quantity;

            // Validar stock para nueva cantidad
            if (!CalculationUtils.hasEnoughStock(product.getStockQty(), newQuantity)) {
                throw new InsufficientStockException(productId, product.getSku(),
                        newQuantity, product.getStockQty());
            }

            existingItem.setQuantity(newQuantity);
        } else {
            // Producto nuevo: crear CartItem y gestión bidireccional
            CartItem newItem = new CartItem();
            newItem.setCartItemId(generateNextCartItemId());
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setQuantity(quantity);
            newItem.setUnitPrice(product.getPrice());
            newItem.setAddedAt(LocalDateTime.now());

            cart.getItems().add(newItem);      // Agregar a colección del carrito
            newItem.setCart(cart);             // Establecer referencia al carrito
        }

        // Actualizar timestamp del carrito
        touchCart(cart);

        // TODO Etapa 06: cartRepository.save(cart);

        return cartMapper.toDTO(cart);
    }

    /**
     * Actualiza la cantidad de un item en el carrito.
     *
     * @param cartId ID del carrito
     * @param productId ID del producto
     * @param newQuantity Nueva cantidad
     * @return CartDTO actualizado
     * @throws EntityNotFoundException si no existe carrito o producto
     * @throws InvalidCartStateException si el carrito no está OPEN
     * @throws InsufficientStockException si no hay stock suficiente
     * @throws ValidationException si el producto no está en el carrito
     */
    public CartDTO updateItemQuantity(Long cartId, Long productId, Integer newQuantity) {
        ValidationUtils.validatePositive(newQuantity, "quantity");

        Cart cart = findCartEntityOrThrow(cartId);
        if (cart.getStatus() != CartStatus.OPEN) {
            throw new InvalidCartStateException(cartId, cart.getStatus(),
                    CartStatus.OPEN, "update item");
        }

        // Buscar item en el carrito
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProduct().getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new ValidationException("Product not found in cart"));

        // Validar stock
        Product product = item.getProduct();
        if (!CalculationUtils.hasEnoughStock(product.getStockQty(), newQuantity)) {
            throw new InsufficientStockException(productId, product.getSku(),
                    newQuantity, product.getStockQty());
        }

        item.setQuantity(newQuantity);
        touchCart(cart);

        // TODO Etapa 06: cartRepository.save(cart);

        return cartMapper.toDTO(cart);
    }

    /**
     * Remueve un item del carrito (gestión bidireccional).
     *
     * @param cartId ID del carrito
     * @param productId ID del producto
     * @return CartDTO actualizado
     * @throws EntityNotFoundException si no existe
     * @throws InvalidCartStateException si el carrito no está OPEN
     * @throws ValidationException si el producto no está en el carrito
     */
    public CartDTO removeItem(Long cartId, Long productId) {
        Cart cart = findCartEntityOrThrow(cartId);
        if (cart.getStatus() != CartStatus.OPEN) {
            throw new InvalidCartStateException(cartId, cart.getStatus(),
                    CartStatus.OPEN, "remove item");
        }

        // Buscar item
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getProduct().getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new ValidationException("Product not found in cart"));

        // Gestión bidireccional
        cart.getItems().remove(item);     // Remover de colección
        item.setCart(null);                // Remover referencia

        touchCart(cart);

        // TODO Etapa 06: cartRepository.save(cart);

        return cartMapper.toDTO(cart);
    }

    /**
     * Limpia todos los items del carrito.
     *
     * @param cartId ID del carrito
     * @throws EntityNotFoundException si no existe
     * @throws InvalidCartStateException si el carrito no está OPEN
     */
    public void clearCart(Long cartId) {
        Cart cart = findCartEntityOrThrow(cartId);
        if (cart.getStatus() != CartStatus.OPEN) {
            throw new InvalidCartStateException(cartId, cart.getStatus(),
                    CartStatus.OPEN, "clear");
        }

        cart.getItems().clear();
        touchCart(cart);

        // TODO Etapa 06: cartRepository.save(cart);
    }

    /**
     * Calcula el total del carrito.
     *
     * @param cartId ID del carrito
     * @return Total del carrito
     * @throws EntityNotFoundException si no existe
     */
    public BigDecimal calculateCartTotal(Long cartId) {
        Cart cart = findCartEntityOrThrow(cartId);
        return cart.calculateTotal(); // Usa método del modelo
    }

    /**
     * ALGORITMO DE CART MERGE (CRÍTICO)
     * ===================================
     * Fusiona carrito de invitado con carrito de usuario registrado.
     *
     * Escenario:
     * - Usuario invitado agrega productos a carrito A
     * - Usuario se registra o inicia sesión
     * - Usuario ya tiene carrito B en su cuenta
     * - Se debe fusionar A → B sin perder productos
     *
     * Proceso:
     * 1. Obtener ambos carritos y validar
     * 2. Para cada item del carrito invitado:
     *    - Si producto existe en carrito usuario: sumar cantidades
     *    - Si producto NO existe: mover item al carrito usuario
     * 3. Marcar carrito invitado como ABANDONED
     * 4. Actualizar timestamps
     *
     * @param guestCartId ID del carrito de invitado
     * @param userId ID del usuario registrado
     * @return CartDTO del carrito fusionado
     * @throws EntityNotFoundException si algún carrito no existe
     * @throws InvalidCartStateException si los carritos no están OPEN
     * @throws CartMergeException si el carrito guest ya tiene usuario asignado
     * @throws InsufficientStockException si no hay stock suficiente para cantidad fusionada
     */
    public CartDTO mergeGuestCartToUserCart(Long guestCartId, Long userId) {
        // 1. Obtener ambos carritos
        Cart guestCart = findCartEntityOrThrow(guestCartId);
        Cart userCart = findOrCreateOpenCartForUser(userId);

        // 2. Validar estados
        if (guestCart.getStatus() != CartStatus.OPEN) {
            throw new InvalidCartStateException(guestCartId, guestCart.getStatus(),
                    CartStatus.OPEN, "merge");
        }
        if (userCart.getStatus() != CartStatus.OPEN) {
            throw new InvalidCartStateException(userCart.getCartId(),
                    userCart.getStatus(), CartStatus.OPEN, "merge");
        }

        // 3. Validar que guestCart sea realmente de invitado
        if (guestCart.getUser() != null) {
            throw new CartMergeException(guestCartId, userCart.getCartId(),
                    "Guest cart already has a user assigned");
        }

        // 4. Fusionar items del carrito invitado al carrito usuario
        for (CartItem guestItem : new ArrayList<>(guestCart.getItems())) {
            Product product = guestItem.getProduct();
            Integer guestQuantity = guestItem.getQuantity();

            // Buscar si el producto ya existe en carrito de usuario
            CartItem userItem = userCart.getItems().stream()
                    .filter(item -> item.getProduct().getProductId()
                            .equals(product.getProductId()))
                    .findFirst()
                    .orElse(null);

            if (userItem != null) {
                // Producto YA existe en carrito usuario: sumar cantidades
                int totalQuantity = userItem.getQuantity() + guestQuantity;

                // Validar stock para cantidad fusionada
                if (!CalculationUtils.hasEnoughStock(product.getStockQty(), totalQuantity)) {
                    throw new InsufficientStockException(product.getProductId(),
                            product.getSku(), totalQuantity, product.getStockQty());
                }

                userItem.setQuantity(totalQuantity);

                // Resolver conflicto de precio: mantener más reciente
                if (guestItem.getAddedAt().isAfter(userItem.getAddedAt())) {
                    userItem.setUnitPrice(guestItem.getUnitPrice());
                }
            } else {
                // Producto NO existe en carrito usuario: mover item
                // Validar stock disponible
                if (!CalculationUtils.hasEnoughStock(product.getStockQty(), guestQuantity)) {
                    throw new InsufficientStockException(product.getProductId(),
                            product.getSku(), guestQuantity, product.getStockQty());
                }

                // Crear nuevo item en carrito de usuario
                CartItem newItem = new CartItem();
                newItem.setCartItemId(generateNextCartItemId());
                newItem.setCart(userCart);
                newItem.setProduct(product);
                newItem.setQuantity(guestQuantity);
                newItem.setUnitPrice(guestItem.getUnitPrice());
                newItem.setAddedAt(guestItem.getAddedAt());

                // Gestión bidireccional
                userCart.getItems().add(newItem);
                newItem.setCart(userCart);
            }
        }

        // 5. Marcar carrito invitado como ABANDONED
        guestCart.setStatus(CartStatus.ABANDONED);
        touchCart(guestCart);

        // 6. Actualizar carrito de usuario
        touchCart(userCart);

        // TODO Etapa 06: cartRepository.save(guestCart);
        // TODO Etapa 06: cartRepository.save(userCart);

        return cartMapper.toDTO(userCart);
    }

    /**
     * Verifica si el carrito está abierto.
     *
     * @param cartId ID del carrito
     * @return true si está OPEN
     */
    public boolean isCartOpen(Long cartId) {
        Cart cart = findCartEntityOrThrow(cartId);
        return cart.isOpen();
    }

    /**
     * Actualiza el timestamp del carrito (touch).
     *
     * @param cartId ID del carrito
     */
    public void touchCartById(Long cartId) {
        Cart cart = findCartEntityOrThrow(cartId);
        touchCart(cart);
        // TODO Etapa 06: cartRepository.save(cart);
    }

    /**
     * Busca entity Cart por ID o lanza excepción.
     *
     * @param cartId ID del carrito
     * @return Cart entity
     * @throws EntityNotFoundException si no existe
     */
    public Cart findCartEntityOrThrow(Long cartId) {
        // TODO Etapa 06: return cartRepository.findById(cartId)
        return cartsInMemory.stream()
                .filter(c -> c.getCartId().equals(cartId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Cart", cartId));
    }

    // Métodos privados auxiliares

    /**
     * Busca carrito OPEN del usuario o crea uno nuevo si no existe.
     */
    private Cart findOrCreateOpenCartForUser(Long userId) {
        User user = userService.findUserEntityOrThrow(userId);

        // Buscar carrito OPEN existente
        Cart cart = cartsInMemory.stream()
                .filter(c -> c.getUser() != null &&
                        c.getUser().getUserId().equals(userId) &&
                        c.getStatus() == CartStatus.OPEN)
                .findFirst()
                .orElse(null);

        if (cart == null) {
            // Crear nuevo carrito
            cart = new Cart();
            cart.setCartId(generateNextId());
            cart.setUser(user);
            cart.setStatus(CartStatus.OPEN);
            cart.setCreatedAt(LocalDateTime.now());
            cart.setUpdatedAt(LocalDateTime.now());

            cartsInMemory.add(cart);
        }

        return cart;
    }

    /**
     * Actualiza el timestamp updatedAt del carrito.
     */
    private void touchCart(Cart cart) {
        cart.setUpdatedAt(LocalDateTime.now());
    }

    // Métodos auxiliares para simular auto-increment
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

