package co.edu.cesde.pps.web.controller;

import co.edu.cesde.pps.model.User;
import co.edu.cesde.pps.service.AddressService;
import co.edu.cesde.pps.web.dto.request.AddressUpsertRequest;
import co.edu.cesde.pps.web.dto.response.AddressResponse;
import co.edu.cesde.pps.web.security.CurrentSessionResolver;
import co.edu.cesde.pps.dto.AddressDTO;
import co.edu.cesde.pps.enums.AddressType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(ApiRoutes.ADDRESSES)
public class AddressController {

    private final AddressService addressService;
    private final CurrentSessionResolver sessionResolver;

    public AddressController(AddressService addressService,
                             CurrentSessionResolver sessionResolver) {
        this.addressService = addressService;
        this.sessionResolver = sessionResolver;
    }

    // GET /api/v1/users/me/addresses
    // Lista todas las direcciones del usuario autenticado
    @GetMapping
    public ResponseEntity<List<AddressResponse>> getMyAddresses(
            HttpServletRequest request) {

        User user = sessionResolver.resolveAuthenticatedUser(request);

        List<AddressResponse> addresses = addressService
                .findUserAddresses(user.getUserId())
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(addresses);
    }

    // GET /api/v1/users/me/addresses/{id}
    // @PathVariable extrae el {id} de la URL
    @GetMapping("/{id}")
    public ResponseEntity<AddressResponse> getAddress(
            @PathVariable Long id,
            HttpServletRequest request) {

        // Verificar que el usuario está autenticado
        User user = sessionResolver.resolveAuthenticatedUser(request);

        AddressDTO address = addressService.findById(id);
        return ResponseEntity.ok(toResponse(address));
    }

    // POST /api/v1/users/me/addresses
    @PostMapping
    public ResponseEntity<AddressResponse> createAddress(
            @RequestBody @Valid AddressUpsertRequest body,
            HttpServletRequest request) {

        User user = sessionResolver.resolveAuthenticatedUser(request);

        // Convertir el request DTO al DTO interno que espera el service
        AddressDTO dto = toDTO(body, user.getUserId());
        AddressDTO created = addressService.addAddress(user.getUserId(), dto);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(toResponse(created));
    }

    // PUT /api/v1/users/me/addresses/{id}
    @PutMapping("/{id}")
    public ResponseEntity<AddressResponse> updateAddress(
            @PathVariable Long id,
            @RequestBody @Valid AddressUpsertRequest body,
            HttpServletRequest request) {

        sessionResolver.resolveAuthenticatedUser(request);

        AddressDTO dto = toDTO(body, null);
        AddressDTO updated = addressService.updateAddress(id, dto);

        return ResponseEntity.ok(toResponse(updated));
    }

    // PATCH /api/v1/users/me/addresses/{id}/default
    // PATCH se usa para actualizaciones parciales (solo cambia isDefault)
    @PatchMapping("/{id}/default")
    public ResponseEntity<AddressResponse> setDefault(
            @PathVariable Long id,
            HttpServletRequest request) {

        User user = sessionResolver.resolveAuthenticatedUser(request);

        AddressDTO updated = addressService.setDefaultAddress(
                user.getUserId(), id);

        return ResponseEntity.ok(toResponse(updated));
    }

    // DELETE /api/v1/users/me/addresses/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAddress(
            @PathVariable Long id,
            HttpServletRequest request) {

        User user = sessionResolver.resolveAuthenticatedUser(request);
        addressService.deleteAddress(user.getUserId(), id);

        // 204 No Content = eliminado exitosamente, sin body
        return ResponseEntity.noContent().build();
    }

    // ── Mappers privados ───────────────────────────────────────────────────

    // Convierte AddressUpsertRequest → AddressDTO interno
    private AddressDTO toDTO(AddressUpsertRequest request, Long userId) {
        AddressDTO dto = new AddressDTO();
        dto.setUserId(userId);
        dto.setType(request.type());
        dto.setLine1(request.line1());
        dto.setLine2(request.line2());
        dto.setCity(request.city());
        dto.setState(request.state());
        dto.setCountry(request.country());
        dto.setPostalCode(request.postalCode());
        dto.setIsDefault(false);
        return dto;
    }

    // Convierte AddressDTO interno → AddressResponse para el frontend
    private AddressResponse toResponse(AddressDTO dto) {
        return new AddressResponse(
                dto.getAddressId(),
                dto.getType(),
                dto.getLine1(),
                dto.getLine2(),
                dto.getCity(),
                dto.getState(),
                dto.getCountry(),
                dto.getPostalCode(),
                Boolean.TRUE.equals(dto.getIsDefault())
        );
    }
}