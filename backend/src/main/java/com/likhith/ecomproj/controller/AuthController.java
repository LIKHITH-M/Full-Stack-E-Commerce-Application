package com.likhith.ecomproj.controller;

import com.likhith.ecomproj.model.User;
import com.likhith.ecomproj.service.JwtService;
import com.likhith.ecomproj.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            User registered = userService.register(user);
            Map<String, String> response = new HashMap<>();
            response.put("message", "User registered successfully");
            response.put("username", registered.getUsername());
            response.put("role", registered.getRole());
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Registration failed: " + e.getMessage());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User user) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()));
            if (authentication.isAuthenticated()) {
                // Fetch full user to get role
                User dbUser = userService.findByUsername(user.getUsername());
                String token = jwtService.generateToken(user.getUsername(), dbUser.getRole());
                Map<String, String> response = new HashMap<>();
                response.put("token", token);
                response.put("username", user.getUsername());
                response.put("email", dbUser.getEmail() != null ? dbUser.getEmail() : "");
                response.put("role", dbUser.getRole());
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
        } catch (Exception e) {
            // fall through
        }
        Map<String, String> response = new HashMap<>();
        response.put("error", "Invalid username or password");
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }
}
