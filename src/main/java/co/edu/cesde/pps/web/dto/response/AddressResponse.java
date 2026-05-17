package co.edu.cesde.pps.web.dto.response;

import co.edu.cesde.pps.enums.AddressType;

public record AddressResponse(
        Long addressId,
        AddressType type,
        String line1,
        String line2,
        String city,
        String state,
        String country,
        String postalCode,
        boolean isDefault
) {}