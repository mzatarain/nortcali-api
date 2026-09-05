package com.nortcali.api.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.nortcali.api.entity.Employee;
import com.nortcali.api.repository.EmployeeRepository;

@Service
public class EmployeeDetailsService implements UserDetailsService {

    private final EmployeeRepository repo;

    public EmployeeDetailsService(EmployeeRepository repo) {
        this.repo = repo;
    }

    /**
     * Fase 2 (plan SaaS): el parámetro {@code username} de la interfaz Spring Security recibe
     * el email (login por email, no por el campo interno {@code Employee.username}) — viene de
     * {@code UsernamePasswordAuthenticationToken} en {@code AuthController.login} y del subject
     * del JWT en {@code JwtAuthFilter}. El principal resultante también queda como el email
     * (no {@code e.getUsername()}), para que {@code SecurityContextHolder...getName()} devuelva
     * siempre el mismo identificador usado para autenticar — todo el código que resuelve al
     * empleado autenticado (AuthController, RestaurantAccessInterceptor, *ServiceImpl) usa
     * {@code findByEmail}/{@code findByEmailWithRestaurants} sobre ese valor.
     */
    @Override
    public UserDetails loadUserByUsername(String email) {
        Employee e = repo.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Employee not found: " + email));

        return User.builder()
            .username(e.getEmail())
            .password(e.getPassword())
            .roles(e.getRole())
            .accountLocked(e.isLocked())
            .disabled(!"ACTIVE".equals(e.getStatus()))
            .build();
    }
}