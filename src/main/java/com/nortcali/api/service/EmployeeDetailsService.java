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

    @Override
    public UserDetails loadUserByUsername(String username) {
        Employee e = repo.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("Employee not found: " + username));

        return User.builder()
            .username(e.getUsername())
            .password(e.getPassword())
            .roles(e.getRole())
            .accountLocked(e.isLocked())
            .disabled(!"ACTIVE".equals(e.getStatus()))
            .build();
    }
}