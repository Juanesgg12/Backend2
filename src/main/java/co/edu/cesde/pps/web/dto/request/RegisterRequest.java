package co.edu.cesde.pps.web.dto.request;

import jakarta.validation.constraints.*;

// @NotBlank valida que el campo no sea null, vacío ni solo espacios
// @Email valida formato de correo electrónico
// @Size valida longitud mínima y máxima
public record RegisterRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String password,

        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        // Teléfono es opcional, por eso no tiene @NotBlank
        String phone
) {}