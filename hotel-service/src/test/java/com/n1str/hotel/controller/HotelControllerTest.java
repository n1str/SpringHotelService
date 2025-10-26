package com.n1str.hotel.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.n1str.hotel.dto.CreateHotelRequest;
import com.n1str.hotel.dto.HotelDto;
import com.n1str.hotel.service.HotelService;
import com.n1str.hotel.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = HotelController.class)
@AutoConfigureMockMvc(addFilters = false)
class HotelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private HotelService hotelService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @org.springframework.boot.test.context.TestConfiguration
    @org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
    static class TestMethodSecurityConfig { }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createHotel_Success() throws Exception {
        CreateHotelRequest request = new CreateHotelRequest("Grand Hotel", "123 Main St", "Luxury hotel", 5);
        HotelDto response = new HotelDto(1L, "Grand Hotel", "123 Main St", "Luxury hotel", 5);

        when(hotelService.createHotel(any())).thenReturn(response);

        mockMvc.perform(post("/api/hotels")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Grand Hotel"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createHotel_Forbidden_WhenNotAdmin() throws Exception {
        CreateHotelRequest request = new CreateHotelRequest("Grand Hotel", "123 Main St", "Luxury hotel", 5);

        mockMvc.perform(post("/api/hotels")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAllHotels_Success() throws Exception {
        List<HotelDto> hotels = Arrays.asList(
                new HotelDto(1L, "Hotel A", "Address A", "Description A", 4),
                new HotelDto(2L, "Hotel B", "Address B", "Description B", 5)
        );

        when(hotelService.getAllHotels()).thenReturn(hotels);

        mockMvc.perform(get("/api/hotels"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Hotel A"));
    }

    
}

