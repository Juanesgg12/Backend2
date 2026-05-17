package co.edu.cesde.pps.web.dto.request;

import jakarta.validation.constraints.*;

public record CheckoutRequest(

        @NotNull(message = "Cart ID is required")
        Long cartId,

        @NotNull(message = "Shipping address ID is required")
        Long shippingAddressId,

        @NotNull(message = "Billing address ID is required")
        Long billingAddressId
) {}