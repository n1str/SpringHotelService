package com.n1str.booking.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.n1str.booking.dto.AuthRequest;
import com.n1str.booking.dto.AuthResponse;
import com.n1str.booking.dto.RegisterRequest;
import com.n1str.booking.service.UserService;
import com.n1str.booking.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    // Mock security filter so SecurityConfig can wire without needing JwtUtil
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void register_Success() throws Exception {
        RegisterRequest request = new RegisterRequest("newuser", "password123", "test@example.com", "Test User");
        AuthResponse response = new AuthResponse("fake-jwt-token", "newuser", "USER");

        when(userService.register(any())).thenReturn(response);

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void register_ValidationFails_WhenUsernameIsShort() throws Exception {
        RegisterRequest request = new RegisterRequest("ab", "password123", "test@example.com", "Test User");

        mockMvc.perform(post("/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void authenticate_Success() throws Exception {
        AuthRequest request = new AuthRequest("testuser", "password123");
        AuthResponse response = new AuthResponse("fake-jwt-token", "testuser", "USER");

        when(userService.authenticate(any())).thenReturn(response);

        mockMvc.perform(post("/user/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    void authenticate_Fails_WithInvalidCredentials() throws Exception {
        AuthRequest request = new AuthRequest("testuser", "wrongpassword");

        when(userService.authenticate(any())).thenThrow(new RuntimeException("Invalid username or password"));

        mockMvc.perform(post("/user/auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }
}

