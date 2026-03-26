package co.edu.cesde.pps.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import co.edu.cesde.pps.model.User;
import java.util.Optional;
public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmailIgnoreCase(String email);
    Optional<User>FindByEmail(String email);
}
