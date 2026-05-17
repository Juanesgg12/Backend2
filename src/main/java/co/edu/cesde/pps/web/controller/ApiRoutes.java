package co.edu.cesde.pps.web.controller;

// Esta clase centraliza todas las rutas de la API.
// Ventaja: si necesitas cambiar /api/v1 a /api/v2,
// solo cambias aquí y todos los controllers se actualizan.
public final class ApiRoutes {

    private ApiRoutes() {} // Clase utilitaria, no se instancia

    public static final String BASE = "/api/v1";

    public static final String AUTH      = BASE + "/auth";
    public static final String USERS     = BASE + "/users";
    public static final String ADDRESSES = BASE + "/users/me/addresses";
    public static final String CART      = BASE + "/cart";
    public static final String ORDERS    = BASE + "/orders";
    public static final String CATEGORIES = BASE + "/categories";
    public static final String PRODUCTS  = BASE + "/products";
    public static final String ADMIN     = BASE + "/admin";
}