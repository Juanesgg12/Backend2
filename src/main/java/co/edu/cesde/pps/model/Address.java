package co.edu.cesde.pps.model;

import co.edu.cesde.pps.enums.AddressType;

import lombok.*;
import java.util.Objects;




/**
 * Entidad Address - Representa direcciones de envío y/o facturación de un usuario.
 *
 * Un usuario puede tener múltiples direcciones (ej: casa, oficina).
 * Cada dirección tiene un tipo: SHIPPING (envío) o BILLING (facturación).
 *
 * Campos:
 * - addressId: Identificador único de la dirección (PK)
 * - user: Usuario propietario de la dirección (N:1 con User)
 * - type: Tipo de dirección (SHIPPING o BILLING)
 * - line1: Línea 1 de dirección (calle, número)
 * - line2: Línea 2 de dirección (apartamento, piso) - opcional
 * - city: Ciudad
 * - state: Estado/Departamento/Provincia
 * - country: País
 * - postalCode: Código postal
 * - isDefault: Indica si es la dirección por defecto del usuario
 *
 * Relaciones:
 * - N:1 con User (muchas direcciones pertenecen a un usuario)
 * - 1:N con Order (como shipping_address_id o billing_address_id)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder



public class Address {

    private Long addressId;
    private User user;
    private AddressType type;
    private String line1;
    private String line2;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    @Builder.Default
    private Boolean isDefault = false;
    // equals y hashCode basados en ID

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Address address = (Address) o;
        return Objects.equals(addressId, address.addressId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(addressId);
    }

    // toString personalizado sin navegación a objetos relacionados (solo IDs)

    @Override
    public String toString() {
        return "Address{" +
                "addressId=" + addressId +
                ", userId=" + (user != null ? user.getUserId() : null) +
                ", type=" + type +
                ", line1='" + line1 + '\'' +
                ", line2='" + line2 + '\'' +
                ", city='" + city + '\'' +
                ", state='" + state + '\'' +
                ", country='" + country + '\'' +
                ", postalCode='" + postalCode + '\'' +
                ", isDefault=" + isDefault +
                '}';
    }
}




