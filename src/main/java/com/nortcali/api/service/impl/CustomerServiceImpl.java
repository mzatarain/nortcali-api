package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.CustomerRequest;
import com.nortcali.api.dto.response.CustomerResponse;
import com.nortcali.api.entity.Customer;
import com.nortcali.api.exception.DuplicateResourceException;
import com.nortcali.api.exception.ResourceNotFoundException;
import com.nortcali.api.mapper.CustomerMapper;
import com.nortcali.api.repository.CustomerRepository;
import com.nortcali.api.repository.RestaurantRepository;
import com.nortcali.api.service.CustomerService;
import com.nortcali.api.util.PhoneNormalizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepo;
    private final RestaurantRepository restaurantRepo;
    private final CustomerMapper mapper;

    public CustomerServiceImpl(CustomerRepository customerRepo,
                               RestaurantRepository restaurantRepo,
                               CustomerMapper mapper) {
        this.customerRepo = customerRepo;
        this.restaurantRepo = restaurantRepo;
        this.mapper = mapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerResponse> getByRestaurant(Long restaurantId) {
        return customerRepo.findByRestaurantIdAndIsActiveTrue(restaurantId)
                .stream().map(mapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getById(Long id) {
        return mapper.toResponse(findOrThrow(id));
    }

    @Override
    public CustomerResponse create(Long restaurantId, CustomerRequest request) {
        restaurantRepo.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));

        String phone = PhoneNormalizer.normalize(request.getPhone());

        // Un teléfono real identifica a un solo cliente por restaurante (el placeholder no cuenta)
        if (!PhoneNormalizer.isPlaceholder(phone)) {
            Optional<Customer> existing =
                    customerRepo.findFirstByRestaurantIdAndPhoneOrderByIdAsc(restaurantId, phone);
            if (existing.isPresent()) {
                Customer customer = existing.get();
                if (customer.isActive()) {
                    throw new DuplicateResourceException(
                            "Ya existe un cliente con el teléfono " + phone + ": " + customer.getFirstName());
                }
                // Cliente desactivado con el mismo teléfono: se reactiva con los datos nuevos en vez de duplicarlo
                mapper.updateEntity(request, customer);
                customer.setPhone(phone);
                customer.setActive(true);
                log.info("Cliente {} reactivado al registrar de nuevo el teléfono {}", customer.getId(), phone);
                return mapper.toResponse(customerRepo.save(customer));
            }
        }

        Customer entity = mapper.toEntity(request);
        entity.setPhone(phone);
        entity.setRestaurant(restaurantRepo.getReferenceById(restaurantId));
        entity.setTotalOrders(0);
        return mapper.toResponse(customerRepo.save(entity));
    }

    @Override
    public CustomerResponse update(Long id, CustomerRequest request) {
        Customer entity = findOrThrow(id);
        String phone = PhoneNormalizer.normalize(request.getPhone());

        Long restaurantId = entity.getRestaurant().getId();
        if (!PhoneNormalizer.isPlaceholder(phone)
                && customerRepo.existsByRestaurantIdAndPhoneAndIdNot(restaurantId, phone, id)) {
            throw new DuplicateResourceException("Ya existe otro cliente con el teléfono " + phone);
        }

        mapper.updateEntity(request, entity);
        entity.setPhone(phone);
        return mapper.toResponse(customerRepo.save(entity));
    }

    @Override
    public void deactivate(Long id) {
        Customer entity = findOrThrow(id);
        entity.setActive(false);
        customerRepo.save(entity);
        log.info("Cliente {} desactivado", id);
    }

    private Customer findOrThrow(Long id) {
        return customerRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }
}
