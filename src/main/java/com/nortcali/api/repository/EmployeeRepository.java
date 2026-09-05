package com.nortcali.api.repository;

import com.nortcali.api.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByUsername(String username);

    List<Employee> findByRestaurantsId(Long restaurantId);

    /**
     * Trae `restaurants` (y su `tenant`) ya inicializados en la misma consulta. Necesario en
     * puntos que acceden a la colección fuera de una transacción propia (p.ej.
     * {@code RestaurantAccessInterceptor.preHandle}, que corre antes de que el
     * `OpenEntityManagerInViewInterceptor` de Spring Boot abra su sesión — el orden entre
     * interceptors no está garantizado) — sin esto, la colección lazy lanza
     * `LazyInitializationException: no session`.
     */
    @Query("SELECT e FROM Employee e LEFT JOIN FETCH e.restaurants r LEFT JOIN FETCH r.tenant WHERE e.username = :username")
    Optional<Employee> findByUsernameWithRestaurants(@Param("username") String username);
}
