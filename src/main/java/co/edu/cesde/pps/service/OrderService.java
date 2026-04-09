package co.edu.cesde.pps.service;

import co.edu.cesde.pps.dto.OrderDTO;
import co.edu.cesde.pps.enums.CartStatus;
import co.edu.cesde.pps.exception.*;
import co.edu.cesde.pps.mapper.OrderMapper;
import co.edu.cesde.pps.model.*;
import co.edu.cesde.pps.repository.OrderRepository;
import co.edu.cesde.pps.repository.OrderStatusRepository;
import co.edu.cesde.pps.util.CalculationUtils;
import co.edu.cesde.pps.config.AppConfig;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderMapper orderMapper;
    private final OrderRepository orderRepository;
    private final OrderStatusRepository orderStatusRepository;
    private final UserService userService;
    private final CartService cartService;
    private final AddressService addressService;
    private final ProductService productService;
    private final Random random;

    public OrderService(OrderRepository orderRepository,
                        OrderStatusRepository orderStatusRepository,
                        UserService userService,
                        CartService cartService,
                        AddressService addressService,
                        ProductService productService) {
        this.orderMapper = new OrderMapper();
        this.orderRepository = orderRepository;
        this.orderStatusRepository = orderStatusRepository;
        this.userService = userService;
        this.cartService = cartService;
        this.addressService = addressService;
        this.productService = productService;
        this.random = new Random();
    }

    @Transactional
    public OrderDTO checkout(Long userId, Long cartId,
                             Long shippingAddressId, Long billingAddressId) {

        userService.findUserEntityOrThrow(userId);

        Cart cart = cartService.findCartEntityOrThrow(cartId);

        if (cart.getStatus() != CartStatus.OPEN) {
            throw new InvalidCartStateException(
                    cartId, cart.getStatus(), CartStatus.OPEN, "checkout");
        }
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new ValidationException("Cannot checkout empty cart");
        }
        if (cart.getUser() == null ||
                !cart.getUser().getUserId().equals(userId)) {
            throw new ValidationException("Cart does not belong to user");
        }

        Address shippingAddress = addressService.findAddressEntityOrThrow(shippingAddressId);
        Address billingAddress  = addressService.findAddressEntityOrThrow(billingAddressId);

        if (!shippingAddress.getUser().getUserId().equals(userId)) {
            throw new ValidationException("Shipping address does not belong to user");
        }
        if (!billingAddress.getUser().getUserId().equals(userId)) {
            throw new ValidationException("Billing address does not belong to user");
        }

        for (CartItem item : cart.getItems()) {
            Product p = item.getProduct();
            if (!p.getIsActive()) {
                throw new ValidationException(
                        "Product '" + p.getName() + "' is no longer available");
            }
            if (!CalculationUtils.hasEnoughStock(p.getStockQty(), item.getQuantity())) {
                throw new InsufficientStockException(
                        p.getProductId(), p.getSku(),
                        item.getQuantity(), p.getStockQty());
            }
        }

        // Obtener el estado inicial de la orden desde la BD
        // "PENDING" debe existir en la tabla order_status
        OrderStatus pendingStatus = orderStatusRepository
                .findByNameIgnoreCase("PENDING")
                .orElseThrow(() -> new EntityNotFoundException(
                        "OrderStatus 'PENDING' not found in database"));

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(cart.getUser())
                .orderStatus(pendingStatus)
                .shippingAddress(shippingAddress)
                .billingAddress(billingAddress)
                .createdAt(LocalDateTime.now())
                .items(new ArrayList<>())
                .build();

        for (CartItem cartItem : cart.getItems()) {
            BigDecimal lineTotal = CalculationUtils.calculateOrderItemLineTotal(
                    cartItem.getUnitPrice(), cartItem.getQuantity());

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(cartItem.getProduct())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(cartItem.getUnitPrice())
                    .lineTotal(lineTotal)
                    .build();

            order.getItems().add(orderItem);
        }

        List<BigDecimal> lineTotals = order.getItems().stream()
                .map(OrderItem::getLineTotal)
                .collect(Collectors.toList());

        BigDecimal subtotal    = CalculationUtils.calculateOrderSubtotal(lineTotals);
        BigDecimal tax         = CalculationUtils.calculateTax(
                subtotal, BigDecimal.valueOf(AppConfig.getDefaultTaxRate()));
        BigDecimal shippingCost = CalculationUtils.calculateShippingCost(subtotal, 1);
        BigDecimal total       = CalculationUtils.calculateOrderTotal(
                subtotal, tax, shippingCost);

        order.setSubtotal(subtotal);
        order.setTax(tax);
        order.setShippingCost(shippingCost);
        order.setTotal(total);

        // Descontar stock de cada producto
        for (CartItem item : cart.getItems()) {
            productService.decreaseStock(
                    item.getProduct().getProductId(), item.getQuantity());
        }

        // Marcar el carrito como convertido
        cart.setStatus(CartStatus.CONVERTED);
        cart.setUpdatedAt(LocalDateTime.now());
        cartService.findCartEntityOrThrow(cartId); // Fuerza flush del cart

        Order saved = orderRepository.save(order);
        return orderMapper.toDTO(saved);
    }

    public OrderDTO findById(Long orderId) {
        return orderMapper.toDTO(findOrderEntityOrThrow(orderId));
    }

    public OrderDTO findByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Order with number: " + orderNumber));
        return orderMapper.toDTO(order);
    }

    public List<OrderDTO> findOrdersByUser(Long userId) {
        userService.findUserEntityOrThrow(userId);
        return orderMapper.toDTOList(orderRepository.findByUserUserId(userId));
    }

    public Order findOrderEntityOrThrow(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Order", orderId));
    }

    private String generateOrderNumber() {
        String prefix = AppConfig.getOrderNumberPrefix();
        String date   = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String rand   = String.format("%06d", random.nextInt(1000000));
        return prefix + date + "-" + rand;
    }
}