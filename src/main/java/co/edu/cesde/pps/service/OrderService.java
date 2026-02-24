package co.edu.cesde.pps.service;

import co.edu.cesde.pps.dto.OrderDTO;
import co.edu.cesde.pps.enums.CartStatus;
import co.edu.cesde.pps.exception.EntityNotFoundException;
import co.edu.cesde.pps.exception.InsufficientStockException;
import co.edu.cesde.pps.exception.InvalidCartStateException;
import co.edu.cesde.pps.exception.ValidationException;
import co.edu.cesde.pps.mapper.OrderMapper;
import co.edu.cesde.pps.model.*;
import co.edu.cesde.pps.util.CalculationUtils;
import co.edu.cesde.pps.config.AppConfig;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Servicio para gestión de órdenes.
 *
 * Responsabilidades:
 * - Proceso de checkout (Cart → Order)
 * - Generar número de orden único
 * - Calcular totales (subtotal, tax, shipping)
 * - Validar direcciones
 * - Actualizar stock de productos
 * - Marcar carrito como CONVERTED
 * - Búsqueda de órdenes
 * - Conversión Entity <-> DTO
 *
 * NOTA: En Etapa 06 se agregará:
 * - @Service annotation
 * - @Transactional (CRÍTICO para checkout)
 * - Inyección de OrderRepository
 * - Persistencia real
 */
public class OrderService {

    private final OrderMapper orderMapper;
    private final UserService userService;
    private final CartService cartService;
    private final AddressService addressService;
    private final ProductService productService;
    // TODO Etapa 06: private final OrderRepository orderRepository;
    private final List<Order> ordersInMemory;
    private final Random random;

    public OrderService(UserService userService, CartService cartService,
                       AddressService addressService, ProductService productService) {
        this.orderMapper = new OrderMapper();
        this.userService = userService;
        this.cartService = cartService;
        this.addressService = addressService;
        this.productService = productService;
        this.ordersInMemory = new ArrayList<>();
        this.random = new Random();
    }

    /**
     * PROCESO DE CHECKOUT (CRÍTICO)
     * ==============================
     * Convierte un carrito en una orden completada.
     *
     * Proceso:
     * 1. Validar usuario registrado
     * 2. Validar carrito (OPEN, no vacío, pertenece al usuario)
     * 3. Validar direcciones existen
     * 4. Verificar disponibilidad y stock de todos los productos
     * 5. Crear orden con número único
     * 6. Copiar items del carrito a la orden (congelar precios)
     * 7. Calcular totales (subtotal, tax, shipping, total)
     * 8. Actualizar stock de productos
     * 9. Marcar carrito como CONVERTED
     *
     * @param userId ID del usuario
     * @param cartId ID del carrito
     * @param shippingAddressId ID de la dirección de envío
     * @param billingAddressId ID de la dirección de facturación
     * @return OrderDTO de la orden creada
     * @throws EntityNotFoundException si no existe usuario, carrito o dirección
     * @throws InvalidCartStateException si el carrito no está OPEN
     * @throws ValidationException si el carrito está vacío o no pertenece al usuario
     * @throws InsufficientStockException si no hay stock suficiente
     */
    public OrderDTO checkout(Long userId, Long cartId, Long shippingAddressId,
                            Long billingAddressId) {
        // 1. Validar usuario está registrado
        userService.findUserEntityOrThrow(userId);

        // 2. Obtener y validar carrito
        Cart cart = cartService.findCartEntityOrThrow(cartId);

        // Validar estado OPEN
        if (cart.getStatus() != CartStatus.OPEN) {
            throw new InvalidCartStateException(cartId, cart.getStatus(),
                CartStatus.OPEN, "checkout");
        }

        // Validar no vacío
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new ValidationException("Cannot checkout empty cart");
        }

        // Validar que pertenece al usuario
        if (cart.getUser() == null || !cart.getUser().getUserId().equals(userId)) {
            throw new ValidationException("Cart does not belong to user");
        }

        // 3. Validar que las direcciones existen
        Address shippingAddress = addressService.findAddressEntityOrThrow(shippingAddressId);
        Address billingAddress = addressService.findAddressEntityOrThrow(billingAddressId);

        // Validar que las direcciones pertenecen al usuario
        if (!shippingAddress.getUser().getUserId().equals(userId)) {
            throw new ValidationException("Shipping address does not belong to user");
        }
        if (!billingAddress.getUser().getUserId().equals(userId)) {
            throw new ValidationException("Billing address does not belong to user");
        }

        // 4. Verificar disponibilidad y stock de todos los productos
        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();

            // Verificar que el producto esté activo
            if (!product.getIsActive()) {
                throw new ValidationException("Product '" + product.getName() +
                    "' is no longer available");
            }

            // Verificar stock suficiente
            if (!CalculationUtils.hasEnoughStock(product.getStockQty(), item.getQuantity())) {
                throw new InsufficientStockException(product.getProductId(),
                    product.getSku(), item.getQuantity(), product.getStockQty());
            }
        }

        // 5. Crear orden con número único
        String orderNumber = generateOrderNumber();
        Order order = new Order();
        order.setOrderId(generateNextId());
        order.setOrderNumber(orderNumber);
        order.setUser(cart.getUser());
        order.setShippingAddress(shippingAddress);
        order.setBillingAddress(billingAddress);
        // orderStatus se asigna en servicio (TODO Etapa 06)
        order.setCreatedAt(LocalDateTime.now());

        // 6. Copiar items del carrito a la orden (congelar precios históricos)
        for (CartItem cartItem : cart.getItems()) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderItemId(generateNextOrderItemId());
            orderItem.setOrder(order);
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(cartItem.getUnitPrice());  // Precio histórico al momento de compra

            // Calcular lineTotal
            orderItem.setLineTotal(CalculationUtils.calculateOrderItemLineTotal(
                cartItem.getUnitPrice(), cartItem.getQuantity()));

            // Gestión bidireccional
            order.getItems().add(orderItem);
            orderItem.setOrder(order);
        }

        // 7. Calcular totales
        List<BigDecimal> lineTotals = order.getItems().stream()
                .map(OrderItem::getLineTotal)
                .collect(Collectors.toList());

        BigDecimal subtotal = CalculationUtils.calculateOrderSubtotal(lineTotals);
        BigDecimal taxRate = BigDecimal.valueOf(AppConfig.getDefaultTaxRate());
        BigDecimal tax = CalculationUtils.calculateTax(subtotal, taxRate);
        BigDecimal shippingCost = calculateShippingCost(subtotal);
        BigDecimal total = CalculationUtils.calculateOrderTotal(subtotal, tax, shippingCost);

        order.setSubtotal(subtotal);
        order.setTax(tax);
        order.setShippingCost(shippingCost);
        order.setTotal(total);

        // 8. Actualizar stock de productos
        for (CartItem item : cart.getItems()) {
            productService.decreaseStock(item.getProduct().getProductId(),
                item.getQuantity());
        }

        // 9. Marcar carrito como CONVERTED
        cart.setStatus(CartStatus.CONVERTED);
        cart.setUpdatedAt(LocalDateTime.now());

        // TODO Etapa 06: orderRepository.save(order);
        // TODO Etapa 06: cartRepository.save(cart);
        ordersInMemory.add(order);

        return orderMapper.toDTO(order);
    }

    /**
     * Busca orden por ID.
     *
     * @param orderId ID de la orden
     * @return OrderDTO
     * @throws EntityNotFoundException si no existe
     */
    public OrderDTO findById(Long orderId) {
        Order order = findOrderEntityOrThrow(orderId);
        return orderMapper.toDTO(order);
    }

    /**
     * Busca orden por número de orden.
     *
     * @param orderNumber Número de orden
     * @return OrderDTO
     * @throws EntityNotFoundException si no existe
     */
    public OrderDTO findByOrderNumber(String orderNumber) {
        // TODO Etapa 06: Order order = orderRepository.findByOrderNumber(orderNumber)
        Order order = ordersInMemory.stream()
                .filter(o -> o.getOrderNumber().equalsIgnoreCase(orderNumber))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Order with number: " + orderNumber));

        return orderMapper.toDTO(order);
    }

    /**
     * Lista todas las órdenes de un usuario.
     *
     * @param userId ID del usuario
     * @return Lista de OrderDTO
     */
    public List<OrderDTO> findOrdersByUser(Long userId) {
        userService.findUserEntityOrThrow(userId); // Validar que existe

        // TODO Etapa 06: List<Order> orders = orderRepository.findByUserId(userId);
        List<Order> userOrders = ordersInMemory.stream()
                .filter(o -> o.getUser() != null && o.getUser().getUserId().equals(userId))
                .collect(Collectors.toList());

        return orderMapper.toDTOList(userOrders);
    }

    /**
     * Lista órdenes por estado.
     *
     * @param statusId ID del estado
     * @return Lista de OrderDTO
     */
    public List<OrderDTO> findOrdersByStatus(Long statusId) {
        // TODO Etapa 06: List<Order> orders = orderRepository.findByOrderStatusId(statusId);
        List<Order> statusOrders = ordersInMemory.stream()
                .filter(o -> o.getOrderStatus() != null && o.getOrderStatus().getOrderStatusId().equals(statusId))
                .collect(Collectors.toList());

        return orderMapper.toDTOList(statusOrders);
    }

    /**
     * Lista órdenes por rango de fechas.
     *
     * @param startDate Fecha inicio
     * @param endDate Fecha fin
     * @return Lista de OrderDTO
     */
    public List<OrderDTO> findOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        // TODO Etapa 06: List<Order> orders = orderRepository.findByCreatedAtBetween(start, end);
        List<Order> rangeOrders = ordersInMemory.stream()
                .filter(o -> o.getCreatedAt().isAfter(startDate) &&
                           o.getCreatedAt().isBefore(endDate))
                .collect(Collectors.toList());

        return orderMapper.toDTOList(rangeOrders);
    }

    /**
     * Genera un número de orden único.
     *
     * Formato: {PREFIX}-YYYYMMDD-XXXXXX
     * Ejemplo: ORD-20260203-123456
     *
     * @return Número de orden único
     */
    public String generateOrderNumber() {
        String prefix = AppConfig.getOrderNumberPrefix(); // "ORD-"
        String date = LocalDateTime.now().format(
            DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = String.format("%06d", random.nextInt(1000000));

        return prefix + date + "-" + randomPart;
    }

    /**
     * Calcula el costo de envío basado en el subtotal.
     *
     * Reglas:
     * - Si subtotal >= threshold: envío gratis
     * - Si subtotal < threshold: costo base
     *
     * @param subtotal Subtotal de la orden
     * @return Costo de envío
     */
    private BigDecimal calculateShippingCost(BigDecimal subtotal) {

        return CalculationUtils.calculateShippingCost(subtotal, 1); // shippingZone = 1 por defecto
    }

    /**
     * Busca entity Order por ID o lanza excepción.
     *
     * @param orderId ID de la orden
     * @return Order entity
     * @throws EntityNotFoundException si no existe
     */
    public Order findOrderEntityOrThrow(Long orderId) {
        // TODO Etapa 06: return orderRepository.findById(orderId)
        return ordersInMemory.stream()
                .filter(o -> o.getOrderId().equals(orderId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Order", orderId));
    }

    // Métodos auxiliares para simular auto-increment
    private Long generateNextId() {
        return ordersInMemory.stream()
                .mapToLong(Order::getOrderId)
                .max()
                .orElse(0L) + 1;
    }

    private Long generateNextOrderItemId() {
        return ordersInMemory.stream()
                .flatMap(order -> order.getItems().stream())
                .mapToLong(OrderItem::getOrderItemId)
                .max()
                .orElse(0L) + 1;
    }
}
