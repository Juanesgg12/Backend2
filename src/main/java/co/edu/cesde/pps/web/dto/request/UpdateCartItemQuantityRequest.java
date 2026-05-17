package co.edu.cesde.pps.web.dto.request;

import jakarta.validation.constraints.*;

public record UpdateCartItemQuantityRequest(

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be greater than zero")
        Integer quantity
) {}
