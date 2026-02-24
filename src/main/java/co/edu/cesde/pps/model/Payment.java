package co.edu.cesde.pps.model;

import co.edu.cesde.pps.enums.Currency;
import co.edu.cesde.pps.util.ValidationUtils;

import lombok.*;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Entidad Payment - Registra transacciones de pago asociadas a una orden.
 *
 * Una orden puede tener múltiples pagos (reintentos, pagos parciales, reembolsos).
 *
 * Campos:
 * - paymentId: Identificador único del pago (PK)
 * - order: Orden asociada (N:1 con Order)
 * - paymentMethod: Método de pago usado (N:1 con PaymentMethod)
 * - paymentStatus: Estado del pago (N:1 con PaymentStatus)
 * - amount: Monto del pago (BigDecimal para precisión)
 * - currency: Moneda del pago (USD, COP, EUR)
 * - providerReference: Referencia del proveedor de pagos (ej: ID de transacción de pasarela)
 * - paidAt: Fecha/hora en que se completó el pago exitosamente
 *
 * Consideraciones de diseño:
 * - Múltiples pagos por orden permiten manejar:
 *   * Reintentos de pago fallido
 *   * Pagos parciales
 *   * Reembolsos (amount negativo)
 * - providerReference crucial para conciliación con pasarelas de pago externas
 * - paidAt puede ser NULL si el pago aún no se ha completado (pending)
 * - Currency enum permite soportar múltiples monedas
 * - BigDecimal en amount para precisión monetaria
 *
 * Relaciones:
 * - N:1 con Order (muchos pagos pertenecen a una orden)
 * - N:1 con PaymentMethod (muchos pagos usan un método)
 * - N:1 con PaymentStatus (muchos pagos tienen un estado)
 */
@Entity
@Table (name = "payment")

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payment_id")
    private Long paymentId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id", nullable = false)
    private PaymentMethod paymentMethod;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_status_id", nullable = false)
    private PaymentStatus paymentStatus;
    @Column (nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Currency currency;
    @Column(name = "provider_reference", length = 100, unique = true)
    private String providerReference;
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // Setter personalizado con validación (override de Lombok)

    public void setAmount(BigDecimal amount) {
        // Validación: amount puede ser negativo (reembolsos), pero no null
        ValidationUtils.validateNotNull(amount, "amount");
        this.amount = amount;
    }

    // Método helper para verificar si el pago está completado
    public boolean isPaid() {
        return paidAt != null;
    }

    // Método helper para verificar si es un reembolso
    public boolean isRefund() {
        return amount != null && amount.compareTo(BigDecimal.ZERO) < 0;
    }

    // equals y hashCode basados en ID

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Payment payment = (Payment) o;
        return Objects.equals(paymentId, payment.paymentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(paymentId);
    }

    // toString sin navegación a objetos relacionados (solo IDs)

    @Override
    public String toString() {
        return "Payment{" +
                "paymentId=" + paymentId +
                ", orderId=" + (order != null ? order.getOrderId() : null) +
                ", paymentMethodId=" + (paymentMethod != null ? paymentMethod.getPaymentMethodId() : null) +
                ", paymentStatusId=" + (paymentStatus != null ? paymentStatus.getPaymentStatusId() : null) +
                ", amount=" + amount +
                ", currency=" + currency +
                ", providerReference='" + providerReference + '\'' +
                ", paidAt=" + paidAt +
                ", isPaid=" + isPaid() +
                ", isRefund=" + isRefund() +
                '}';
    }
}
