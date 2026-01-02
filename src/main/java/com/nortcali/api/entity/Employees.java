package com.nortcali.api.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "employees")
public class Employees {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String email;

    private String password;

    private String phone;

    private String role;

    private LocalDate joiningDate;

    private String city;
    
    @Override
    public String toString() {
    	return "Employees [email=" + email + ", name=" + name + ", password=" + password +"]";
    }
    
    public Employees() {
    	
    }
    
    public Employees(String name, String email, String password, String phone, LocalDate joiningDate, String city) {
    	super();
    	this.name = name;
    	this.email = email;
    	this.password = password;
        this.phone = phone;
        this.joiningDate = joiningDate;
        this.city = city;
    }

    // --------- Getters and Setters ----------

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDate getJoiningDate() {
        return joiningDate;
    }

    public void setJoiningDate(LocalDate joiningDate) {
        this.joiningDate = joiningDate;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }
}
