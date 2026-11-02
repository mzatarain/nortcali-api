package com.nortcali.api.service.impl;

import com.nortcali.api.dto.request.CustomerRequest;
import com.nortcali.api.entity.Customer;
import com.nortcali.api.entity.Restaurant;
import com.nortcali.api.exception.BusinessRuleException;
import com.nortcali.api.exception.DuplicateResourceException;
import com.nortcali.api.mapper.CustomerMapper;
import com.nortcali.api.repository.CustomerRepository;
import com.nortcali.api.repository.RestaurantRepository;
import com.nortcali.api.util.PhoneNormalizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceImplTest {

    @Mock CustomerRepository customerRepo;
    @Mock RestaurantRepository restaurantRepo;
    @Mock CustomerMapper mapper;

    @InjectMocks
    CustomerServiceImpl service;

    private static final Long RESTAURANT_ID = 1L;

    // ── helpers ────────────────────────────────────────────────────────────────

    private Restaurant restaurant() {
        Restaurant r = new Restaurant();
        r.setId(RESTAURANT_ID);
        return r;
    }

    private CustomerRequest request(String name, String phone) {
        CustomerRequest req = new CustomerRequest();
        req.setFirstName(name);
        req.setPhone(phone);
        return req;
    }

    private Customer customer(Long id, String name, String phone, boolean active) {
        Customer c = new Customer();
        c.setId(id);
        c.setFirstName(name);
        c.setPhone(phone);
        c.setActive(active);
        c.setRestaurant(restaurant());
        return c;
    }

    private void restaurantExists() {
        when(restaurantRepo.findById(RESTAURANT_ID)).thenReturn(Optional.of(restaurant()));
    }

    // ── create ─────────────────────────────────────────────────────────────────

    @Test
    void create_phoneWithSpacesAndCountryCode_savesTenDigits() {
        restaurantExists();
        when(customerRepo.findFirstByRestaurantIdAndPhoneOrderByIdAsc(RESTAURANT_ID, "6862279910"))
                .thenReturn(Optional.empty());
        when(mapper.toEntity(any())).thenAnswer(inv -> {
            CustomerRequest r = inv.getArgument(0);
            return customer(null, r.getFirstName(), r.getPhone(), true);
        });
        when(customerRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.create(RESTAURANT_ID, request("Manuel", "+52 686 227 9910"));

        ArgumentCaptor<Customer> saved = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepo).save(saved.capture());
        assertThat(saved.getValue().getPhone()).isEqualTo("6862279910");
    }

    @Test
    void create_phoneOfActiveCustomer_throwsDuplicate() {
        restaurantExists();
        when(customerRepo.findFirstByRestaurantIdAndPhoneOrderByIdAsc(RESTAURANT_ID, "6862279910"))
                .thenReturn(Optional.of(customer(7L, "Manuel", "6862279910", true)));

        assertThatThrownBy(() -> service.create(RESTAURANT_ID, request("Otro", "686 227 9910")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("6862279910");
        verify(customerRepo, never()).save(any());
    }

    @Test
    void create_phoneOfInactiveCustomer_reactivatesInsteadOfDuplicating() {
        restaurantExists();
        Customer inactive = customer(7L, "Alex", "6863871755", false);
        when(customerRepo.findFirstByRestaurantIdAndPhoneOrderByIdAsc(RESTAURANT_ID, "6863871755"))
                .thenReturn(Optional.of(inactive));
        when(customerRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.create(RESTAURANT_ID, request("Alex Rendon", "686 387 1755"));

        verify(mapper, never()).toEntity(any());
        verify(mapper).updateEntity(any(), eq(inactive));
        assertThat(inactive.isActive()).isTrue();
        assertThat(inactive.getPhone()).isEqualTo("6863871755");
        verify(customerRepo).save(inactive);
    }

    @Test
    void create_withoutPhone_usesPlaceholderAndSkipsDuplicateCheck() {
        restaurantExists();
        when(mapper.toEntity(any())).thenAnswer(inv -> customer(null, "Sin tel", null, true));
        when(customerRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.create(RESTAURANT_ID, request("Sin tel", PhoneNormalizer.NO_PHONE_PLACEHOLDER));

        verify(customerRepo, never()).findFirstByRestaurantIdAndPhoneOrderByIdAsc(anyLong(), anyString());
        ArgumentCaptor<Customer> saved = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepo).save(saved.capture());
        assertThat(saved.getValue().getPhone()).isEqualTo(PhoneNormalizer.NO_PHONE_PLACEHOLDER);
    }

    @Test
    void create_invalidPhone_throwsBusinessRule() {
        restaurantExists();

        assertThatThrownBy(() -> service.create(RESTAURANT_ID, request("Clara", "clara")))
                .isInstanceOf(BusinessRuleException.class);
        verify(customerRepo, never()).save(any());
    }

    // ── update ─────────────────────────────────────────────────────────────────

    @Test
    void update_normalizesPhone() {
        Customer existing = customer(5L, "Raul", "6869460015", true);
        when(customerRepo.findById(5L)).thenReturn(Optional.of(existing));
        when(customerRepo.existsByRestaurantIdAndPhoneAndIdNot(RESTAURANT_ID, "6869460016", 5L)).thenReturn(false);
        when(customerRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.update(5L, request("Raul Farias", "686 946 0016"));

        assertThat(existing.getPhone()).isEqualTo("6869460016");
    }

    @Test
    void update_phoneOfAnotherCustomer_throwsDuplicate() {
        Customer existing = customer(5L, "Raul", "6869460015", true);
        when(customerRepo.findById(5L)).thenReturn(Optional.of(existing));
        when(customerRepo.existsByRestaurantIdAndPhoneAndIdNot(RESTAURANT_ID, "6862279910", 5L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(5L, request("Raul", "6862279910")))
                .isInstanceOf(DuplicateResourceException.class);
        verify(customerRepo, never()).save(any());
    }
}
