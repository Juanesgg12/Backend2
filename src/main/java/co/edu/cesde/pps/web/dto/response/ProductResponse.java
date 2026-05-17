package co.edu.cesde.pps.web.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(
        Long productId,
        String sku,
        String name,
        String description,
        BigDecimal price,
        Integer stockQty,
        boolean isActive,
        boolean isAvailable,
        Long categoryId,
        String categoryName,
        LocalDateTime createdAt
) {}