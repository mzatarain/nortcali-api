package com.nortcali.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "restaurants")
public class Restaurant {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 120)
    private String name;
    
    @Column(length = 20)
    private String phone;
    
    @Column(length = 20)
    private String whatsapp;
    
    @Column(name = "address_line", length = 255)
    private String addressLine;
    
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;


    /* =========================
       RELATIONS
       ========================= */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "city_id", nullable = false)
    private City city;
    
    /* 
     * CONSTRUCTORS
     */
    
    public Restaurant() {
    	
    }
    
    public Restaurant(Long id, String name, String phone, String whatsapp, String addressLine, boolean isActive, City city) {
    	super();
    	this.id = id;
    	this.name = name;
    	this.phone = phone;
    	this.whatsapp = whatsapp;
    	this.addressLine = addressLine;
    	this.isActive = isActive;
    	this.city = city;
    }
    
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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getWhatsapp() {
        return whatsapp;
    }

    public void setWhatsapp(String whatsapp) {
        this.whatsapp = whatsapp;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public void setAddressLine(String addressLine) {
        this.addressLine = addressLine;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }
    
    public City getCity() {
    	return city;
    }
    
    public void setCity(City city) {
    	this.city = city;
    }
}