package co.edu.cesde.pps.web.dto.response;

import java.math.BigDecimal;

public record OrderTotalsResponse(
        BigDecimal subtotal,
        BigDecimal tax,
        BigDecimal shippingCost,
        BigDecimal total
) {}