package co.edu.cesde.pps.web.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long orderId,
        String orderNumber,
        String orderStatus,
        UserResponse user,
        AddressResponse shippingAddress,
        AddressResponse billingAddress,
        List<OrderItemResponse> items,
        OrderTotalsResponse totals,
        LocalDateTime createdAt
) {}