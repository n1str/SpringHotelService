package com.n1str.booking.service;

import com.n1str.booking.dto.AuthRequest;
import com.n1str.booking.dto.AuthResponse;
import com.n1str.booking.dto.RegisterRequest;
import com.n1str.booking.entity.User;
import com.n1str.booking.repository.UserRepository;
import com.n1str.booking.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserService userService;

    @Test
    void register_Success() {
        // Arrange
        RegisterRequest request = new RegisterRequest("newuser", "password123", "test@example.com", "Test User");
        
        when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded_password");
        when(userRepository.save(any())).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(jwtUtil.generateToken(anyString(), any())).thenReturn("fake-jwt-token");

        // Act
        AuthResponse response = userService.register(request);

        // Assert
        assertNotNull(response);
        assertEquals("newuser", response.getUsername());
        assertEquals("USER", response.getRole());
        assertEquals("fake-jwt-token", response.getToken());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_ThrowsException_WhenUsernameExists() {
        // Arrange
        RegisterRequest request = new RegisterRequest("existinguser", "password123", "test@example.com", "Test User");
        
        when(userRepository.existsByUsername(request.getUsername())).thenReturn(true);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> userService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void authenticate_Success() {
        // Arrange
        AuthRequest request = new AuthRequest("testuser", "password123");
        
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("encoded_password");
        user.setRole("USER");
        
        when(userRepository.findByUsername(request.getUsername())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(true);
        when(jwtUtil.generateToken(anyString(), any())).thenReturn("fake-jwt-token");

        // Act
        AuthResponse response = userService.authenticate(request);

        // Assert
        assertNotNull(response);
        assertEquals("testuser", response.getUsername());
        assertEquals("USER", response.getRole());
        assertEquals("fake-jwt-token", response.getToken());
    }

    @Test
    void authenticate_ThrowsException_WithInvalidPassword() {
        // Arrange
        AuthRequest request = new AuthRequest("testuser", "wrongpassword");
        
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setPassword("encoded_password");
        user.setRole("USER");
        
        when(userRepository.findByUsername(request.getUsername())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(request.getPassword(), user.getPassword())).thenReturn(false);

        // Act & Assert
        assertThrows(RuntimeException.class, () -> userService.authenticate(request));
    }

    @Test
    void authenticate_ThrowsException_WhenUserNotFound() {
        // Arrange
        AuthRequest request = new AuthRequest("nonexistent", "password123");
        
        when(userRepository.findByUsername(request.getUsername())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> userService.authenticate(request));
    }
}

