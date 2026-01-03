package com.nortcali.api.logindto;

import jakarta.persistence.Column;

public class LoginDto {
        @Column(unique = true)
    private String email;
    private String password;
	
	
	public LoginDto(String email, String password) {
		super();
		this.email = email;
		this.password = password;
	}
	
	// Getter and Setter for employee email
	public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
    // Getter and Setter for password
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    
    
    @Override
    public String toString() {
    	return "LoginDto [email="+ email + ", password=" + password +"]";
    }
    
    public LoginDto() {}
	
}
