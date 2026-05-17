package co.edu.cesde.pps.web.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductUpsertRequest(

        @NotNull(message = "Category ID is required")
        Long categoryId,

        @NotBlank(message = "SKU is required")
        String sku,

        @NotBlank(message = "Product name is required")
        String name,

        // Descripción opcional
        String description,

        // @PositiveOrZero acepta 0 y positivos (precio puede ser 0)
        @NotNull(message = "Price is required")
        @PositiveOrZero(message = "Price must be zero or greater")
        BigDecimal price,

        @NotNull(message = "Stock quantity is required")
        @PositiveOrZero(message = "Stock quantity must be zero or greater")
        Integer stockQty,

        @NotNull(message = "isActive is required")
        Boolean isActive
) {}