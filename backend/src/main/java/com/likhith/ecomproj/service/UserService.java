package com.likhith.ecomproj.service;

import com.likhith.ecomproj.dto.UserResponseDTO;
import com.likhith.ecomproj.model.User;
import com.likhith.ecomproj.repo.UserRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserService {

    @Autowired
    private UserRepo userRepo;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    /**
     * Registers a new user. ALWAYS forces role to "USER" regardless of client input.
     * This prevents privilege escalation where a malicious client could POST role: "ADMIN".
     */
    public User register(User user) {
        user.setPassword(encoder.encode(user.getPassword()));
        // SECURITY FIX: Always force USER role on public registration.
        // Admin accounts must be created through a separate, protected mechanism.
        user.setRole("USER");
        log.info("Registering new user: {}", user.getUsername());
        return userRepo.save(user);
    }

    public User findByUsername(String username) {
        return userRepo.findByUsername(username);
    }

    public List<User> getAllUsers() {
        return userRepo.findAll();
    }

    /**
     * Returns all users as DTOs, excluding password hashes.
     */
    public List<UserResponseDTO> getAllUsersDTO() {
        return userRepo.findAll().stream()
                .map(u -> new UserResponseDTO(u.getId(), u.getUsername(), u.getEmail(), u.getRole()))
                .collect(Collectors.toList());
    }

    public User getUserById(int id) {
        return userRepo.findById(id).orElse(null);
    }

    public void deleteUser(int id) {
        userRepo.deleteById(id);
    }

    public static UserResponseDTO toDTO(User user) {
        return new UserResponseDTO(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }
}
