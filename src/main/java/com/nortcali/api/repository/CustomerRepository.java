package com.nortcali.api.repository;

import com.nortcali.api.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByRestaurantIdAndIsActiveTrue(Long restaurantId);

    Optional<Customer> findFirstByRestaurantIdAndPhoneOrderByIdAsc(Long restaurantId, String phone);

    boolean existsByRestaurantIdAndPhoneAndIdNot(Long restaurantId, String phone, Long id);
}
