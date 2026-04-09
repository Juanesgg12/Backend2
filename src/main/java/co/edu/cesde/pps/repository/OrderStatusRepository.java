package co.edu.cesde.pps.repository;

import co.edu.cesde.pps.model.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OrderStatusRepository extends JpaRepository<OrderStatus, Long> {

    Optional<OrderStatus> findByNameIgnoreCase(String name);
}