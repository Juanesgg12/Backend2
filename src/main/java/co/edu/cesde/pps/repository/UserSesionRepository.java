package co.edu.cesde.pps.repository;

import co.edu.cesde.pps.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserSesionRepository extends JpaRepository<UserSession, Long> {

    Optional<UserSession> findBySessionToken(String sessionToken);

    boolean existsBySessionToken(String sessionToken);
}