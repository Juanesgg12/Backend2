package co.edu.cesde.pps.web.controller;

import co.edu.cesde.pps.dto.OrderDTO;
import co.edu.cesde.pps.dto.AddressDTO;
import co.edu.cesde.pps.dto.OrderItemDTO;
import co.edu.cesde.pps.model.User;
import co.edu.cesde.pps.service.OrderService;
import co.edu.cesde.pps.web.dto.request.CheckoutRequest;
import co.edu.cesde.pps.web.dto.response.*;
import co.edu.cesde.pps.web.security.CurrentSessionResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiRoutes.ORDERS)
public class OrderController {

    private final OrderService orderService;
    private final CurrentSessionResolver sessionResolver;

    public OrderController(OrderService orderService,
                           CurrentSessionResolver sessionResolver) {
        this.orderService = orderService;
        this.sessionResolver = sessionResolver;
    }

    // POST /api/v1/orders/checkout
    @PostMapping("/checkout")
    public ResponseEntity<OrderResponse> checkout(
            @RequestBody @Valid CheckoutRequest body,
            HttpServletRequest request) {

        User user = sessionResolver.resolveAuthenticatedUser(request);

        OrderDTO order = orderService.checkout(
                user.getUserId(),
                body.cartId(),
                body.shippingAddressId(),
                body.billingAddressId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(order));
    }

    // GET /api/v1/orders/me
    // Lista todas las órdenes del usuario autenticado
    @GetMapping("/me")
    public ResponseEntity<List<OrderResponse>> getMyOrders(
            HttpServletRequest request) {

        User user = sessionResolver.resolveAuthenticatedUser(request);

        List<OrderResponse> orders = orderService
                .findOrdersByUser(user.getUserId())
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(orders);
    }

    // GET /api/v1/orders/{id}
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable Long id,
            HttpServletRequest request) {

        sessionResolver.resolveAuthenticatedUser(request);

        OrderDTO order = orderService.findById(id);
        return ResponseEntity.ok(toResponse(order));
    }

    // ── Mappers privados ───────────────────────────────────────────────────

    private OrderResponse toResponse(OrderDTO dto) {
        List<OrderItemResponse> items = dto.getItems().stream()
                .map(this::toItemResponse)
                .toList();

        OrderTotalsResponse totals = new OrderTotalsResponse(
                dto.getSubtotal(),
                dto.getTax(),
                dto.getShippingCost(),
                dto.getTotal()
        );

        UserResponse user = new UserResponse(
                dto.getUserId(),
                dto.getUserEmail(),
                null, null,
                dto.getUserFullName(),
                null, null, null, null
        );

        return new OrderResponse(
                dto.getOrderId(),
                dto.getOrderNumber(),
                dto.getOrderStatusName(),
                user,
                toAddressResponse(dto.getShippingAddress()),
                toAddressResponse(dto.getBillingAddress()),
                items,
                totals,
                dto.getCreatedAt()
        );
    }

    private OrderItemResponse toItemResponse(OrderItemDTO dto) {
        return new OrderItemResponse(
                dto.getOrderItemId(),
                dto.getProductId(),
                dto.getProductName(),
                dto.getProductSku(),
                dto.getQuantity(),
                dto.getUnitPrice(),
                dto.getLineTotal()
        );
    }

    private AddressResponse toAddressResponse(AddressDTO dto) {
        if (dto == null) return null;
        return new AddressResponse(
                dto.getAddressId(),
                dto.getType(),
                dto.getLine1(),
                dto.getLine2(),
                dto.getCity(),
                dto.getState(),
                dto.getCountry(),
                dto.getPostalCode(),
                Boolean.TRUE.equals(dto.getIsDefault())
        );
    }
}