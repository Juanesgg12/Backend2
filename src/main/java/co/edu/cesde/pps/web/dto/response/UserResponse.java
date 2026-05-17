package co.edu.cesde.pps.web.dto.response;

import co.edu.cesde.pps.enums.UserStatus;
import java.time.LocalDateTime;

public record UserResponse(
        Long userId,
        String email,
        String firstName,
        String lastName,
        String fullName,
        String phone,
        String roleName,
        UserStatus status,
        LocalDateTime createdAt
) {}