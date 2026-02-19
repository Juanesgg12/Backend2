package co.edu.cesde.pps.model;
import jakarta.persistence.*;
import lombok.*;
import java.util.Objects;

/**
 * Entidad OrderStatus - Catálogo de estados posibles de una orden.
 *
 * Ejemplos: pending, paid, shipped, delivered, cancelled
 *
 * Campos:
 * - orderStatusId: Identificador único del estado (PK)
 * - name: Nombre único del estado (UNIQUE)
 *
 * Relaciones (futuro - etapa02):
 * - 1:N con Order (un estado puede aplicar a múltiples órdenes)
 */
@Entity
@Table(name = "order_status")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name= "order_status_id")
    private Long orderStatusId;
    @Column(name= "name", nullable = false, unique = true, length = 50)
    private String name;
    @Column (name= "description", length = 255)
    private String description;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderStatus that = (OrderStatus) o;
        return Objects.equals(orderStatusId, that.orderStatusId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderStatusId);
    }

    // toString sin navegación a objetos relacionados

    @Override
    public String toString() {
        return "OrderStatus{" +
                "orderStatusId=" + orderStatusId +
                ", name='" + name + '\'' +
                '}';
    }
}
