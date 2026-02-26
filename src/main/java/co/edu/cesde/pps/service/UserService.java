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

public class UserService {

    private final UserMapper userMapper;
    private final List<User> usersInMemory;

    public UserService() {
        this.userMapper = new UserMapper();
        this.usersInMemory = new ArrayList<>();
    }

    public UserDTO registerUser(String email, String passwordHash, String firstName,
                                String lastName, String phone) {

        ValidationUtils.validateEmail(email, "email");
        ValidationUtils.validateNotBlank(passwordHash, "passwordHash");
        ValidationUtils.validateMinLength(passwordHash, AppConfig.getMinPasswordLength(), "password");
        ValidationUtils.validateNotBlank(firstName, "firstName");
        ValidationUtils.validateNotBlank(lastName, "lastName");

        if (phone != null && !phone.isBlank()) {
            ValidationUtils.validatePhone(phone, "phone");
        }

        if (existsByEmail(email)) {
            throw new DuplicateEntityException("User", "email", email);
        }

        // Crear rol por defecto
        Role defaultRole = Role.builder()
                .roleId(2L)
                .name("CUSTOMER")
                .build();

        // Crear usuario con Builder
        User user = User.builder()
                .userId(generateNextId())
                .role(defaultRole)
                .email(email.toLowerCase().trim())
                .passwordHash(passwordHash)
                .firstName(firstName.trim())
                .lastName(lastName.trim())
                .phone(phone != null ? phone.trim() : null)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        usersInMemory.add(user);

        return userMapper.toDTO(user);
    }

    public UserDTO findById(Long userId) {
        User user = findUserEntityOrThrow(userId);
        return userMapper.toDTO(user);
    }

    public UserDTO findByEmail(String email) {
        User user = usersInMemory.stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("User with email: " + email));

        return userMapper.toDTO(user);
    }

    public List<UserDTO> findAllUsers() {
        return userMapper.toDTOList(usersInMemory);
    }

    public UserDTO updateProfile(Long userId, String firstName, String lastName, String phone) {

        User user = findUserEntityOrThrow(userId);

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

        return userMapper.toDTO(user);
    }

    public void deleteUser(Long userId) {
        User user = findUserEntityOrThrow(userId);
        user.setStatus(UserStatus.INACTIVE);
    }

    public boolean existsByEmail(String email) {
        return usersInMemory.stream()
                .anyMatch(u -> u.getEmail().equalsIgnoreCase(email));
    }

    public User findUserEntityOrThrow(Long userId) {
        return usersInMemory.stream()
                .filter(u -> u.getUserId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("User", userId));
    }

    private Long generateNextId() {
        return usersInMemory.stream()
                .mapToLong(User::getUserId)
                .max()
                .orElse(0L) + 1;
    }
}
