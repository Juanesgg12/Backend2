package co.edu.cesde.pps.web.dto.response;

import java.util.List;

public record CategoryResponse(
        Long categoryId,
        String name,
        String slug,
        Long parentId,
        String parentName,
        boolean isRoot,
        List<CategoryResponse> subcategories
) {}