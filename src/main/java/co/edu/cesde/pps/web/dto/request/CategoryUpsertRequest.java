package co.edu.cesde.pps.web.dto.request;

import jakarta.validation.constraints.*;

public record CategoryUpsertRequest(

        @NotBlank(message = "Category name is required")
        String name,

        // slug es opcional, si no viene se genera desde el name
        String slug,

        // parentId es opcional, null = categoría raíz
        Long parentId
) {}