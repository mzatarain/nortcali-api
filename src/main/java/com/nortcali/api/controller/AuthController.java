package com.nortcali.api.controller;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nortcali.api.dto.LoginRequest;
import com.nortcali.api.dto.LoginResponse;
import com.nortcali.api.security.JwtUtil;

@RestController
//@CrossOrigin("http://localhost:5173/signup")
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtUtil jwt;

    public AuthController(AuthenticationManager am, JwtUtil jwt) {
        this.authManager = am;
        this.jwt = jwt;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {

    	authManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getUsername(),
                request.getPassword()
            )
        );

        String token = jwt.generateToken(request.getUsername());

        return new LoginResponse(
            token,
            request.getUsername(),
            request.getRole()
        );
    }
}


