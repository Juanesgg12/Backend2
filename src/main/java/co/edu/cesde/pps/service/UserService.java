package co.edu.cesde.pps.service;

import co.edu.cesde.pps.dto.UserDTO;
import co.edu.cesde.pps.exception.DuplicateEntityException;
import co.edu.cesde.pps.exception.EntityNotFoundException;
import co.edu.cesde.pps.mapper.UserMapper;
import co.edu.cesde.pps.model.Role;
import co.edu.cesde.pps.model.User;
import co.edu.cesde.pps.util.ValidationUtils;
import co.edu.cesde.pps.config.AppConfig;
import co.edu.cesde.pps.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio para gestión de usuarios.
 *
 * Responsabilidades:
 * - CRUD de usuarios
 * - Registro con validaciones
 * - Búsqueda por diferentes criterios
 * - Conversión Entity <-> DTO
 *
 * NOTA: En Etapa 06 se agregará:
 * - @Service annotation
 * - @Transactional
 * - Inyección de UserRepository
 * - Persistencia real
 */
public class UserService {

    private final UserMapper userMapper;
    // TODO Etapa 06: private final UserRepository userRepository;
    // Por ahora trabajamos con lista en memoria
    private final List<User> usersInMemory;

    public UserService() {
        this.userMapper = new UserMapper();
        this.usersInMemory = new ArrayList<>();
    }

    /**
     * Registra un nuevo usuario.
     *
     * @param email Email del usuario
     * @param passwordHash Hash de la contraseña
     * @param firstName Nombre
     * @param lastName Apellido
     * @param phone Teléfono (opcional)
     * @return UserDTO del usuario creado
     * @throws DuplicateEntityException si el email ya existe
     */
    public UserDTO registerUser(String email, String passwordHash, String firstName,
                                String lastName, String phone) {
        // Validaciones
        ValidationUtils.validateEmail(email, "email");
        ValidationUtils.validateNotBlank(passwordHash, "passwordHash");
        ValidationUtils.validateMinLength(passwordHash, AppConfig.getMinPasswordLength(), "password");
        ValidationUtils.validateNotBlank(firstName, "firstName");
        ValidationUtils.validateNotBlank(lastName, "lastName");

        if (phone != null && !phone.isBlank()) {
            ValidationUtils.validatePhone(phone, "phone");
        }

        // Verificar email duplicado
        if (existsByEmail(email)) {
            throw new DuplicateEntityException("User", "email", email);
        }

        // Crear usuario
        // TODO Etapa 06: cargar Role desde BD
        Role defaultRole = new Role();
        defaultRole.setRoleId(2L); // CUSTOMER
        defaultRole.setName("CUSTOMER");

        User user = new User();
        user.setUserId(generateNextId()); // Simula auto-increment
        user.setRole(defaultRole);
        user.setEmail(email.toLowerCase().trim());
        user.setPasswordHash(passwordHash);
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setPhone(phone != null ? phone.trim() : null);
        user.setStatus(UserStatus.ACTIVE);
        user.setCreatedAt(LocalDateTime.now());

        // TODO Etapa 06: userRepository.save(user);
        usersInMemory.add(user);

        return userMapper.toDTO(user);
    }

    /**
     * Busca usuario por ID.
     *
     * @param userId ID del usuario
     * @return UserDTO
     * @throws EntityNotFoundException si no existe
     */
    public UserDTO findById(Long userId) {
        User user = findUserEntityOrThrow(userId);
        return userMapper.toDTO(user);
    }

    /**
     * Busca usuario por email.
     *
     * @param email Email del usuario
     * @return UserDTO
     * @throws EntityNotFoundException si no existe
     */
    public UserDTO findByEmail(String email) {
        // TODO Etapa 06: User user = userRepository.findByEmail(email)
        User user = usersInMemory.stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("User with email: " + email));

        return userMapper.toDTO(user);
    }

    /**
     * Lista todos los usuarios.
     *
     * @return Lista de UserDTO
     */
    public List<UserDTO> findAllUsers() {
        // TODO Etapa 06: List<User> users = userRepository.findAll();
        return userMapper.toDTOList(usersInMemory);
    }

    /**
     * Actualiza perfil de usuario.
     *
     * @param userId ID del usuario
     * @param firstName Nuevo nombre
     * @param lastName Nuevo apellido
     * @param phone Nuevo teléfono
     * @return UserDTO actualizado
     * @throws EntityNotFoundException si no existe
     */
    public UserDTO updateProfile(Long userId, String firstName, String lastName, String phone) {
        User user = findUserEntityOrThrow(userId);

        // Validaciones
        if (firstName != null) {
            ValidationUtils.validateNotBlank(firstName, "firstName");
            user.setFirstName(firstName.trim());
        }

        if (lastName != null) {
            ValidationUtils.validateNotBlank(lastName, "lastName");
            user.setLastName(lastName.trim());
        }

        if (phone != null) {
            if (!phone.isBlank()) {
                ValidationUtils.validatePhone(phone, "phone");
                user.setPhone(phone.trim());
            } else {
                user.setPhone(null);
            }
        }

        // TODO Etapa 06: userRepository.save(user);

        return userMapper.toDTO(user);
    }

    /**
     * Elimina un usuario (soft delete cambiando estado).
     *
     * @param userId ID del usuario
     * @throws EntityNotFoundException si no existe
     */
    public void deleteUser(Long userId) {
        User user = findUserEntityOrThrow(userId);
        user.setStatus(UserStatus.INACTIVE);
        // TODO Etapa 06: userRepository.save(user);
    }

    /**
     * Verifica si existe un usuario con el email dado.
     *
     * @param email Email a verificar
     * @return true si existe
     */
    public boolean existsByEmail(String email) {
        // TODO Etapa 06: return userRepository.existsByEmail(email);
        return usersInMemory.stream()
                .anyMatch(u -> u.getEmail().equalsIgnoreCase(email));
    }

    /**
     * Busca entity User por ID o lanza excepción.
     * Método interno para uso de otros servicios.
     *
     * @param userId ID del usuario
     * @return User entity
     * @throws EntityNotFoundException si no existe
     */
    public User findUserEntityOrThrow(Long userId) {
        // TODO Etapa 06: return userRepository.findById(userId)
        //     .orElseThrow(() -> new EntityNotFoundException("User", userId));
        return usersInMemory.stream()
                .filter(u -> u.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("User", userId));
    }

    // Método auxiliar para simular auto-increment en memoria
    private Long generateNextId() {
        return usersInMemory.stream()
                .mapToLong(User::getUserId)
                .max()
                .orElse(0L) + 1;
    }
}
