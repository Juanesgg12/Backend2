package co.edu.cesde.pps.web.dto.request;

import jakarta.validation.constraints.*;

public record MergeGuestCartRequest(

        @NotNull(message = "Guest cart ID is required")
        Long guestCartId
) {}