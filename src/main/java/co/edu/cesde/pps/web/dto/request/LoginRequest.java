package co.edu.cesde.pps.web.dto.request;

import jakarta.validation.constraints.*;

public record LoginRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {}