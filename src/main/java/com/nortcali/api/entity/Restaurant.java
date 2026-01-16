package com.nortcali.api.entity;

import jakarta.persistence.Entity;
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

    private String name;
    private String phone;
    private String whatsapp;
    private String addressLine;
    private boolean isActive;

    @ManyToOne @JoinColumn(name = "country_id")
    private Country country;

    @ManyToOne @JoinColumn(name = "state_id")
    private State state;

    @ManyToOne @JoinColumn(name = "city_id")
    private City city;
    
    public Restaurant() {
    	
    }
    
    public Restaurant(Long id, String name, String phone, String whatsapp, String addressLine, boolean isActive) {
    	super();
    	this.id = id;
    	this.name = name;
    	this.phone = phone;
    	this.whatsapp = whatsapp;
    	this.addressLine = addressLine;
    	this.isActive = isActive;
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
}