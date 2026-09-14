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

    /**
     * Fase 2 (plan SaaS): identificador de login — reemplaza a {@code findByUsername} en todo
     * el flujo de autenticación (login, JWT, SecurityContext). {@code username} se conserva
     * como campo interno del empleado, sin uso en auth desde este cambio.
     */
    Optional<Employee> findByEmail(String email);

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

    /** Fase 2: misma necesidad de fetch-join que {@code findByUsernameWithRestaurants}, por email. */
    @Query("SELECT e FROM Employee e LEFT JOIN FETCH e.restaurants r LEFT JOIN FETCH r.tenant WHERE e.email = :email")
    Optional<Employee> findByEmailWithRestaurants(@Param("email") String email);

    /** Fase 8 (plan SaaS): misma necesidad de fetch-join, por id — usado al resolver el tenant para impersonar. */
    @Query("SELECT e FROM Employee e LEFT JOIN FETCH e.restaurants r LEFT JOIN FETCH r.tenant WHERE e.id = :id")
    Optional<Employee> findByIdWithRestaurants(@Param("id") Long id);

    /** Fase 8 (plan SaaS): panel de superadmin — empleados de cualquier restaurante del tenant, sin duplicados. */
    @Query("SELECT DISTINCT e FROM Employee e JOIN e.restaurants r WHERE r.tenant.id = :tenantId ORDER BY e.firstName, e.lastName")
    List<Employee> findByRestaurantsTenantId(@Param("tenantId") Long tenantId);

    /** Fase 8 (plan SaaS): métrica "uso" del listado de tenants — cuenta sin duplicar por multi-sucursal. */
    @Query("SELECT COUNT(DISTINCT e) FROM Employee e JOIN e.restaurants r WHERE r.tenant.id = :tenantId")
    long countDistinctByRestaurantsTenantId(@Param("tenantId") Long tenantId);

    /** Fase 8 (plan SaaS): métrica "última conexión" del listado de tenants. */
    @Query("SELECT MAX(e.lastLogin) FROM Employee e JOIN e.restaurants r WHERE r.tenant.id = :tenantId")
    java.time.LocalDateTime findMaxLastLoginByRestaurantsTenantId(@Param("tenantId") Long tenantId);
}
