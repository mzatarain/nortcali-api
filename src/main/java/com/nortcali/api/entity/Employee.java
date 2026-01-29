package com.nortcali.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "employees")
public class Employee {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String password;
    
    @Column(nullable = false, length = 20)
    private String role; // Admin, Manager, Staff
    
    @Column(nullable = false, length = 20)
    private String status; //Active, Inactive
    
    @Column(name = "isLocked",nullable = false)
    private boolean locked = false;
    
    /* ==============
     * RELATIONSHIPS
     */
    @ManyToMany
    @JoinTable(
    		name = "employee_restaurants",
    		joinColumns = @JoinColumn(name = "restaurant_id")
    		)
    private Set<Restaurant> restaurants = new HashSet<>();
    
    
//    @ManyToOne
//    @JoinColumn(name = "restaurant_id")
//    private Restaurant restaurant;
//
//    public Employee() {
//    	
//    }
//    
//    public Employee(Long id, String username, String password, String role, String status, boolean isLocked, Restaurant restaurant) {
//    	super();
//    	this.id = id;
//    	this.username = username;
//    	this.password = password;
//    	this.role = role;
//    	this.status = status;
//    	this.Locked = isLocked;
//    	this.restaurant = restaurant;
//    }
    
    
    /* =================
     * GETTERS AND SETTERS
     */
    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public Set<Restaurant> getRestaurant() {
        return restaurants;
    }

    public void setRestaurant(Set<Restaurant> restaurants) {
        this.restaurants = restaurants;
    }
    

}