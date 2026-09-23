package com.nortcali.api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Fase 12 (plan SaaS): request de edición de empleado, separado de {@link EmployeeRequest}
 * (usado solo en creación). Dos diferencias respecto a ese: no lleva {@code username} — el
 * usuario es inmutable en edición — y {@code password} es opcional (se conserva la actual si
 * llega vacío/ausente).
 */
public class EmployeeUpdateRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String firstName;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100, message = "El apellido no puede superar los 100 caracteres")
    private String lastName;

    @Pattern(regexp = "^$|^(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9]).{10,}$",
             message = "La contraseña debe tener mínimo 10 caracteres, una mayúscula, un número y un carácter especial")
    private String password;

    @Size(max = 20, message = "El teléfono no puede superar los 20 caracteres")
    private String phone;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato válido")
    @Size(max = 150, message = "El email no puede superar los 150 caracteres")
    private String email;

    @NotBlank(message = "El rol es obligatorio")
    @Pattern(regexp = "ADMIN|MANAGER|CASHIER|WAITER|KITCHEN|DELIVERY|TENANT_ADMIN",
             message = "El rol debe ser uno de: ADMIN, MANAGER, CASHIER, WAITER, KITCHEN, DELIVERY, TENANT_ADMIN")
    private String role;

    private String status = "ACTIVE";

    @NotNull(message = "La fecha de contratación es obligatoria")
    private LocalDate hireDate;

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }
}
