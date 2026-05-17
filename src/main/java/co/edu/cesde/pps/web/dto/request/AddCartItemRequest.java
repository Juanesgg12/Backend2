package co.edu.cesde.pps.web.dto.request;

import jakarta.validation.constraints.*;

public record AddCartItemRequest(

        @NotNull(message = "Product ID is required")
        Long productId,

        // @Positive valida que el número sea mayor que cero (1, 2, 3...)
        // No acepta 0 ni negativos
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be greater than zero")
        Integer quantity
) {}