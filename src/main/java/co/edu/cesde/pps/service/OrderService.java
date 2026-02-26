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

public class OrderService {

    private final OrderMapper orderMapper;
    private final UserService userService;
    private final CartService cartService;
    private final AddressService addressService;
    private final ProductService productService;

    private final List<Order> ordersInMemory;
    private final Random random;

    public OrderService(UserService userService,
                        CartService cartService,
                        AddressService addressService,
                        ProductService productService) {
        this.orderMapper = new OrderMapper();
        this.userService = userService;
        this.cartService = cartService;
        this.addressService = addressService;
        this.productService = productService;
        this.ordersInMemory = new ArrayList<>();
        this.random = new Random();
    }

    public OrderDTO checkout(Long userId,
                             Long cartId,
                             Long shippingAddressId,
                             Long billingAddressId) {

        // 1. Validar usuario
        userService.findUserEntityOrThrow(userId);

        // 2. Validar carrito
        Cart cart = cartService.findCartEntityOrThrow(cartId);

        if (cart.getStatus() != CartStatus.OPEN) {
            throw new InvalidCartStateException(cartId, cart.getStatus(),
                    CartStatus.OPEN, "checkout");
        }

        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new ValidationException("Cannot checkout empty cart");
        }

        if (cart.getUser() == null || !cart.getUser().getUserId().equals(userId)) {
            throw new ValidationException("Cart does not belong to user");
        }

        // 3. Validar direcciones
        Address shippingAddress = addressService.findAddressEntityOrThrow(shippingAddressId);
        Address billingAddress = addressService.findAddressEntityOrThrow(billingAddressId);

        if (!shippingAddress.getUser().getUserId().equals(userId)) {
            throw new ValidationException("Shipping address does not belong to user");
        }

        if (!billingAddress.getUser().getUserId().equals(userId)) {
            throw new ValidationException("Billing address does not belong to user");
        }

        // 4. Validar stock
        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();

            if (!product.getIsActive()) {
                throw new ValidationException(
                        "Product '" + product.getName() + "' is no longer available");
            }

            if (!CalculationUtils.hasEnoughStock(
                    product.getStockQty(), item.getQuantity())) {
                throw new InsufficientStockException(
                        product.getProductId(),
                        product.getSku(),
                        item.getQuantity(),
                        product.getStockQty());
            }
        }

        // 5. Crear orden con Builder
        String orderNumber = generateOrderNumber();

        Order order = Order.builder()
                .orderId(generateNextId())
                .orderNumber(orderNumber)
                .user(cart.getUser())
                .shippingAddress(shippingAddress)
                .billingAddress(billingAddress)
                .createdAt(LocalDateTime.now())
                .items(new ArrayList<>())
                .build();

        // 6. Copiar items usando Builder
        for (CartItem cartItem : cart.getItems()) {

            BigDecimal lineTotal =
                    CalculationUtils.calculateOrderItemLineTotal(
                            cartItem.getUnitPrice(),
                            cartItem.getQuantity());

            OrderItem orderItem = OrderItem.builder()
                    .orderItemId(generateNextOrderItemId())
                    .order(order)
                    .product(cartItem.getProduct())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(cartItem.getUnitPrice())
                    .lineTotal(lineTotal)
                    .build();

            order.getItems().add(orderItem);
        }

        // 7. Calcular totales
        List<BigDecimal> lineTotals = order.getItems().stream()
                .map(OrderItem::getLineTotal)
                .collect(Collectors.toList());

        BigDecimal subtotal = CalculationUtils.calculateOrderSubtotal(lineTotals);
        BigDecimal taxRate = BigDecimal.valueOf(AppConfig.getDefaultTaxRate());
        BigDecimal tax = CalculationUtils.calculateTax(subtotal, taxRate);
        BigDecimal shippingCost = calculateShippingCost(subtotal);
        BigDecimal total = CalculationUtils.calculateOrderTotal(
                subtotal, tax, shippingCost);

        order.setSubtotal(subtotal);
        order.setTax(tax);
        order.setShippingCost(shippingCost);
        order.setTotal(total);

        // 8. Actualizar stock
        for (CartItem item : cart.getItems()) {
            productService.decreaseStock(
                    item.getProduct().getProductId(),
                    item.getQuantity());
        }

        // 9. Marcar carrito como CONVERTED
        cart.setStatus(CartStatus.CONVERTED);
        cart.setUpdatedAt(LocalDateTime.now());

        ordersInMemory.add(order);

        return orderMapper.toDTO(order);
    }

    public OrderDTO findById(Long orderId) {
        return orderMapper.toDTO(findOrderEntityOrThrow(orderId));
    }

    public OrderDTO findByOrderNumber(String orderNumber) {
        Order order = ordersInMemory.stream()
                .filter(o -> o.getOrderNumber().equalsIgnoreCase(orderNumber))
                .findFirst()
                .orElseThrow(() ->
                        new EntityNotFoundException("Order with number: " + orderNumber));

        return orderMapper.toDTO(order);
    }

    public List<OrderDTO> findOrdersByUser(Long userId) {
        userService.findUserEntityOrThrow(userId);

        List<Order> userOrders = ordersInMemory.stream()
                .filter(o -> o.getUser() != null &&
                        o.getUser().getUserId().equals(userId))
                .collect(Collectors.toList());

        return orderMapper.toDTOList(userOrders);
    }

    public Order findOrderEntityOrThrow(Long orderId) {
        return ordersInMemory.stream()
                .filter(o -> o.getOrderId().equals(orderId))
                .findFirst()
                .orElseThrow(() ->
                        new EntityNotFoundException("Order", orderId));
    }

    public String generateOrderNumber() {
        String prefix = AppConfig.getOrderNumberPrefix();
        String date = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart =
                String.format("%06d", random.nextInt(1000000));

        return prefix + date + "-" + randomPart;
    }

    private BigDecimal calculateShippingCost(BigDecimal subtotal) {
        return CalculationUtils.calculateShippingCost(subtotal, 1);
    }

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
