package com.likhith.ecomproj.service;

import com.likhith.ecomproj.dto.UserResponseDTO;
import com.likhith.ecomproj.model.User;
import com.likhith.ecomproj.repo.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class UserServiceSecurityTest {

    @Mock
    private UserRepo userRepo;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Security: Public registration must ALWAYS set role to USER even if client supplies ADMIN")
    public void testRegistrationRoleEscalationPrevented() {
        User maliciousRequest = new User();
        maliciousRequest.setUsername("hacker");
        maliciousRequest.setPassword("secret123");
        maliciousRequest.setEmail("hacker@example.com");
        maliciousRequest.setRole("ADMIN"); // Attempting privilege escalation!

        when(userRepo.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User savedUser = userService.register(maliciousRequest);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepo, times(1)).save(userCaptor.capture());

        User capturedUser = userCaptor.getValue();
        assertEquals("USER", capturedUser.getRole(), "Registration MUST override client-provided role to 'USER'");
        assertEquals("USER", savedUser.getRole());
        assertNotEquals("ADMIN", capturedUser.getRole());
    }

    @Test
    @DisplayName("Security: UserResponseDTO and getAllUsersDTO must NEVER expose password or password hash")
    public void testSensitiveFieldsNotExposed() {
        User user = new User();
        user.setId(101);
        user.setUsername("alice");
        user.setEmail("alice@test.com");
        user.setPassword("$2a$12$e8s3YxZ893abcdefghijk.somethingHashed"); // BCrypt hash
        user.setRole("USER");

        when(userRepo.findAll()).thenReturn(List.of(user));

        List<UserResponseDTO> dtos = userService.getAllUsersDTO();
        assertEquals(1, dtos.size());

        UserResponseDTO dto = dtos.get(0);
        assertEquals("alice", dto.getUsername());
        assertEquals("alice@test.com", dto.getEmail());
        assertEquals("USER", dto.getRole());

        // Verify UserResponseDTO class has no password field at all via reflection
        boolean hasPasswordField = false;
        for (var field : UserResponseDTO.class.getDeclaredFields()) {
            if (field.getName().toLowerCase().contains("password")) {
                hasPasswordField = true;
                break;
            }
        }
        assertFalse(hasPasswordField, "UserResponseDTO must not have any password field");
    }

    @Test
    @DisplayName("Security: JwtService fails safely on missing or insecurely short secret")
    public void testJwtSecretValidation() {
        JwtService jwtService = new JwtService();

        // 1. Missing secret must throw IllegalStateException
        jwtService.setSecretKey(null);
        assertThrows(IllegalStateException.class, jwtService::validateJwtSecret);

        jwtService.setSecretKey("   ");
        assertThrows(IllegalStateException.class, jwtService::validateJwtSecret);

        // 2. Secret shorter than 256 bits (32 bytes) must throw IllegalStateException
        jwtService.setSecretKey("short_secret_12345");
        assertThrows(IllegalStateException.class, jwtService::validateJwtSecret);

        // 3. Valid 256-bit secret must succeed
        jwtService.setSecretKey("dGVzdF9qd3Rfc2VjcmV0X2tleV9mb3JfdGVzdGluZ19tdXN0X2JlX2F0X2xlYXN0XzI1Nl9iaXRz");
        assertDoesNotThrow(jwtService::validateJwtSecret);
    }
}
