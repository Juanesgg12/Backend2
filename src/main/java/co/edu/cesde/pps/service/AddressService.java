package co.edu.cesde.pps.service;

import co.edu.cesde.pps.dto.AddressDTO;
import co.edu.cesde.pps.exception.EntityNotFoundException;
import co.edu.cesde.pps.exception.ValidationException;
import co.edu.cesde.pps.mapper.AddressMapper;
import co.edu.cesde.pps.model.Address;
import co.edu.cesde.pps.model.User;
import co.edu.cesde.pps.repository.AddressRepository;
import co.edu.cesde.pps.util.ValidationUtils;
import co.edu.cesde.pps.config.AppConfig;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AddressService {

    private final AddressMapper addressMapper;
    private final AddressRepository addressRepository;
    private final UserService userService;

    public AddressService(AddressRepository addressRepository,
                          UserService userService) {
        this.addressMapper = new AddressMapper();
        this.addressRepository = addressRepository;
        this.userService = userService;
    }

    @Transactional
    public AddressDTO addAddress(Long userId, AddressDTO addressDTO) {
        User user = userService.findUserEntityOrThrow(userId);

        // Contar cuántas direcciones tiene ya este usuario en la BD
        long currentCount = addressRepository.findByUserUserId(userId).size();
        if (currentCount >= AppConfig.getMaxAddressesPerUser()) {
            throw new ValidationException("User has reached maximum number of addresses ("
                    + AppConfig.getMaxAddressesPerUser() + ")");
        }

        validateAddressData(addressDTO);

        Address address = addressMapper.toEntity(addressDTO);
        address.setUser(user);

        // Si es la primera dirección, automáticamente es la default
        if (currentCount == 0) {
            address.setIsDefault(true);
        } else if (Boolean.TRUE.equals(address.getIsDefault())) {
            // Si se quiere marcar como default, primero desmarcar las otras
            // @Modifying + @Query en el repositorio hace el UPDATE en un solo SQL
            addressRepository.unsetDefaultByUserId(userId);
        }

        Address saved = addressRepository.save(address);
        return addressMapper.toDTO(saved);
    }

    @Transactional
    public AddressDTO updateAddress(Long addressId, AddressDTO addressDTO) {
        Address address = findAddressEntityOrThrow(addressId);
        validateAddressData(addressDTO);

        address.setType(addressDTO.getType());
        address.setLine1(addressDTO.getLine1());
        address.setLine2(addressDTO.getLine2());
        address.setCity(addressDTO.getCity());
        address.setState(addressDTO.getState());
        address.setCountry(addressDTO.getCountry());
        address.setPostalCode(addressDTO.getPostalCode());

        if (Boolean.TRUE.equals(addressDTO.getIsDefault()) &&
                !Boolean.TRUE.equals(address.getIsDefault())) {
            addressRepository.unsetDefaultByUserId(address.getUser().getUserId());
            address.setIsDefault(true);
        }

        Address saved = addressRepository.save(address);
        return addressMapper.toDTO(saved);
    }

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        User user = userService.findUserEntityOrThrow(userId);
        Address address = findAddressEntityOrThrow(addressId);

        if (!address.getUser().getUserId().equals(userId)) {
            throw new ValidationException("Address does not belong to user");
        }

        addressRepository.delete(address);

        // Si era la default, promover la siguiente dirección disponible
        List<Address> remaining = addressRepository.findByUserUserId(userId);
        if (Boolean.TRUE.equals(address.getIsDefault()) && !remaining.isEmpty()) {
            remaining.get(0).setIsDefault(true);
            addressRepository.save(remaining.get(0));
        }
    }

    @Transactional
    public AddressDTO setDefaultAddress(Long userId, Long addressId) {
        userService.findUserEntityOrThrow(userId);
        Address address = findAddressEntityOrThrow(addressId);

        if (!address.getUser().getUserId().equals(userId)) {
            throw new ValidationException("Address does not belong to user");
        }

        addressRepository.unsetDefaultByUserId(userId);
        address.setIsDefault(true);
        Address saved = addressRepository.save(address);
        return addressMapper.toDTO(saved);
    }

    public List<AddressDTO> findUserAddresses(Long userId) {
        userService.findUserEntityOrThrow(userId);
        return addressMapper.toDTOList(addressRepository.findByUserUserId(userId));
    }

    public AddressDTO findById(Long addressId) {
        return addressMapper.toDTO(findAddressEntityOrThrow(addressId));
    }

    public Address findAddressEntityOrThrow(Long addressId) {
        return addressRepository.findById(addressId)
                .orElseThrow(() -> new EntityNotFoundException("Address", addressId));
    }

    private void validateAddressData(AddressDTO dto) {
        ValidationUtils.validateNotNull(dto.getType(), "type");
        ValidationUtils.validateNotBlank(dto.getLine1(), "line1");
        ValidationUtils.validateNotBlank(dto.getCity(), "city");
        ValidationUtils.validateNotBlank(dto.getState(), "state");
        ValidationUtils.validateNotBlank(dto.getCountry(), "country");
        ValidationUtils.validateNotBlank(dto.getPostalCode(), "postalCode");
    }
}