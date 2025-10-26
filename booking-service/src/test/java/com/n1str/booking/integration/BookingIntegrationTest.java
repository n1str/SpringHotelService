package com.n1str.booking.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.n1str.booking.dto.AuthResponse;
import com.n1str.booking.dto.RoomDto;
import com.n1str.booking.client.HotelServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
class BookingIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private HotelServiceClient hotelServiceClient;

    @Autowired
    private ObjectMapper objectMapper;

    private String getBaseUrl() {
        return "http://localhost:" + port;
    }

    @Test
    void bookingFlow_ShouldCompleteSuccessfully() {
        // 1. Authenticate as admin
        AuthResponse authResponse = authenticate("admin", "admin123");
        assertNotNull(authResponse.getToken());
        assertEquals("ADMIN", authResponse.getRole());

        // 2. Get user token for booking
        // Register a fresh test user to ensure credentials exist in isolated H2
        String regJson = String.format("""
                {
                    "username": "itest_%d",
                    "password": "pass1234",
                    "email": "itest@example.com",
                    "fullName": "ITest User"
                }
                """, System.currentTimeMillis());

        HttpHeaders regHeaders = new HttpHeaders();
        regHeaders.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        ResponseEntity<AuthResponse> regResp = restTemplate.postForEntity(
                getBaseUrl() + "/user/register", new HttpEntity<>(regJson, regHeaders), AuthResponse.class);
        assertEquals(201, regResp.getStatusCode().value());
        AuthResponse userAuth = regResp.getBody();
        assertNotNull(userAuth);
        assertEquals("USER", userAuth.getRole());

        // 3. Stub Hotel Service client to simulate available room
        RoomDto room = new RoomDto();
        room.setId(1L);
        room.setHotelId(1L);
        room.setAvailable(true);
        room.setTimesBooked(0);
        room.setPricePerNight(150.0);

        org.mockito.Mockito.when(hotelServiceClient.getRecommendedRooms())
                .thenReturn(java.util.List.of(room));
        org.mockito.Mockito.when(hotelServiceClient.getRoomById(1L))
                .thenReturn(room);
        org.mockito.Mockito.doNothing().when(hotelServiceClient)
                .confirmAvailability(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any());
        org.mockito.Mockito.doNothing().when(hotelServiceClient)
                .incrementTimesBooked(1L);

        // 4. Create booking (autoSelect)
        String bookingJson = String.format("""
                {
                    "startDate": "%s",
                    "endDate": "%s",
                    "autoSelect": true,
                    "requestId": "integration-test-%d"
                }
                """, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), System.currentTimeMillis());

        HttpHeaders userHeaders = new HttpHeaders();
        userHeaders.setBearerAuth(userAuth.getToken());
        userHeaders.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

        HttpEntity<String> bookingEntity = new HttpEntity<>(bookingJson, userHeaders);
        ResponseEntity<String> bookingResponse = restTemplate.postForEntity(
                getBaseUrl() + "/booking", bookingEntity, String.class);

        assertTrue(bookingResponse.getStatusCode().value() == 201 || bookingResponse.getStatusCode().value() == 200,
                "Booking should succeed, got: " + bookingResponse.getStatusCode());

        // 5. Verify booking was created
        ResponseEntity<String> bookingsResponse = restTemplate.exchange(
                getBaseUrl() + "/bookings",
                HttpMethod.GET,
                new HttpEntity<>(userHeaders),
                String.class);

        assertEquals(200, bookingsResponse.getStatusCode().value());
        String bookingsBody = bookingsResponse.getBody();
        assertNotNull(bookingsBody);
        assertTrue(bookingsBody.contains("PENDING") || bookingsBody.contains("CONFIRMED"));
    }

    private AuthResponse authenticate(String username, String password) {
        String authJson = String.format("""
                {
                    "username": "%s",
                    "password": "%s"
                }
                """, username, password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(authJson, headers);
        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                getBaseUrl() + "/user/auth", entity, AuthResponse.class);

        assertEquals(200, response.getStatusCode().value());
        return response.getBody();
    }
}