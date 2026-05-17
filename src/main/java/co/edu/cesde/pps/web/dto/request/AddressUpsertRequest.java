package co.edu.cesde.pps.web.dto.request;

import co.edu.cesde.pps.enums.AddressType;
import jakarta.validation.constraints.*;

// "Upsert" = sirve tanto para crear (POST) como para actualizar (PUT)
// Cuando un mismo shape sirve para ambas operaciones, se usa un solo DTO
public record AddressUpsertRequest(

        @NotNull(message = "Address type is required")
        AddressType type,

        @NotBlank(message = "Line 1 is required")
        String line1,

        // line2 es opcional
        String line2,

        @NotBlank(message = "City is required")
        String city,

        @NotBlank(message = "State is required")
        String state,

        @NotBlank(message = "Country is required")
        String country,

        @NotBlank(message = "Postal code is required")
        String postalCode
) {}