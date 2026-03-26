package co.edu.cesde.pps.service;

import co.edu.cesde.pps.dto.UserDTO;
import co.edu.cesde.pps.exception.DuplicateEntityException;
import co.edu.cesde.pps.exception.EntityNotFoundException;
import co.edu.cesde.pps.mapper.UserMapper;
import co.edu.cesde.pps.model.Role;
import co.edu.cesde.pps.model.User;
import co.edu.cesde.pps.repository.RoleRepository;
import co.edu.cesde.pps.repository.UserRepository;
import co.edu.cesde.pps.util.ValidationUtils;
import co.edu.cesde.pps.config.AppConfig;
import co.edu.cesde.pps.enums.UserStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userMapper = new UserMapper();
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional
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

        // Obtener rol por defecto (CUSTOMER con roleId = 2)
        Role defaultRole = roleRepository.findById(2L)
                .orElseThrow(() -> new EntityNotFoundException("Role", 2L));

        // Crear usuario con Builder
        User user = User.builder()
                .role(defaultRole)
                .email(email.toLowerCase().trim())
                .passwordHash(passwordHash)
                .firstName(firstName.trim())
                .lastName(lastName.trim())
                .phone(phone != null ? phone.trim() : null)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        // Guardar en la base de datos
        user = userRepository.save(user);

        return userMapper.toDTO(user);
    }

    public UserDTO findById(Long userId) {
        User user = findUserEntityOrThrow(userId);
        return userMapper.toDTO(user);
    }

    public UserDTO findByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User with email: " + email));

        return userMapper.toDTO(user);
    }

    public List<UserDTO> findAllUsers() {
        return userMapper.toDTOList(userRepository.findAll());
    }

    @Transactional
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

        // Guardar cambios en la base de datos
        user = userRepository.save(user);
        return userMapper.toDTO(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = findUserEntityOrThrow(userId);
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmailIgnoreCase(email);
    }

    public User findUserEntityOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", userId));
    }

}
