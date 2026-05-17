package co.edu.cesde.pps.web.dto.response;

import co.edu.cesde.pps.enums.CartStatus;
import java.time.LocalDateTime;
import java.util.List;

public record CartResponse(
        Long cartId,
        CartStatus status,
        boolean isGuest,
        List<CartItemResponse> items,
        CartSummaryResponse summary,
        LocalDateTime updatedAt
) {}