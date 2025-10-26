package com.n1str.booking.service;

import com.n1str.booking.dto.CreateBookingRequest;
import com.n1str.booking.dto.RoomDto;
import com.n1str.booking.entity.Booking;
import com.n1str.booking.entity.User;
import com.n1str.booking.repository.BookingRepository;
import com.n1str.booking.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
// no need to use FeignException in unit test; use RuntimeException instead
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

	@Mock
	private BookingRepository bookingRepository;

	@Mock
    private UserService userService;

	@Mock
	private com.n1str.booking.client.HotelServiceClient hotelServiceClient;

	@InjectMocks
	private BookingService bookingService;

	private User testUser;
	private CreateBookingRequest testRequest;
	private Authentication authentication;

	@BeforeEach
	void setUp() {
		testUser = new User();
		testUser.setId(1L);
		testUser.setUsername("testuser");
		testUser.setRole("USER");

		testRequest = new CreateBookingRequest();
		testRequest.setStartDate(LocalDate.now().plusDays(1));
		testRequest.setEndDate(LocalDate.now().plusDays(2));
		testRequest.setAutoSelect(true);
        // requestId is optional for this unit test scenario

        // Mock authentication (lenient to avoid unnecessary stubbing errors)
        authentication = mock(Authentication.class, withSettings().lenient());
        when(authentication.getName()).thenReturn("testuser");

        SecurityContext securityContext = mock(SecurityContext.class, withSettings().lenient());
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
	}

	@Test
	void createBooking_ShouldSucceed_WhenAutoSelectAndNoConflicts() {
		// Given
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(hotelServiceClient.getRecommendedRooms()).thenReturn(createTestRooms());
        when(hotelServiceClient.getRoomById(1L)).thenReturn(createTestRoomDto());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// When
		assertDoesNotThrow(() -> bookingService.createBooking("testuser", testRequest));

		// Then
		verify(bookingRepository, times(2)).save(any(Booking.class)); // PENDING + CONFIRMED
		verify(hotelServiceClient).confirmAvailability(eq(1L), any());
		verify(hotelServiceClient).incrementTimesBooked(1L);
	}

	@Test
	void createBooking_ShouldFail_WhenNoAvailableRooms() {
		// Given
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(hotelServiceClient.getRecommendedRooms()).thenReturn(Collections.emptyList());

		// When & Then
		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> bookingService.createBooking("testuser", testRequest));
		assertEquals("Нет доступных номеров", exception.getMessage());
	}

	@Test
    void createBooking_ShouldCompensate_WhenHotelServiceFails() {
		// Given
        when(userService.getUserByUsername("testuser")).thenReturn(testUser);
        when(hotelServiceClient.getRecommendedRooms()).thenReturn(createTestRooms());
        when(hotelServiceClient.getRoomById(1L)).thenReturn(createTestRoomDto());
        doThrow(new RuntimeException("Service unavailable"))
                .when(hotelServiceClient).confirmAvailability(eq(1L), any());
        when(bookingRepository.save(any(Booking.class))).thenAnswer(invocation -> invocation.getArgument(0));

		// When
        assertThrows(RuntimeException.class, () -> bookingService.createBooking("testuser", testRequest));

		// Then
		verify(bookingRepository, times(2)).save(any(Booking.class)); // PENDING + CANCELLED
		verify(hotelServiceClient, never()).incrementTimesBooked(any());
	}

	private List<RoomDto> createTestRooms() {
		RoomDto room = new RoomDto();
		room.setId(1L);
		room.setHotelId(1L);
		room.setAvailable(true);
		room.setTimesBooked(0);
		room.setPricePerNight(100.0);
		return Collections.singletonList(room);
	}

	private RoomDto createTestRoomDto() {
		RoomDto room = new RoomDto();
		room.setId(1L);
		room.setHotelId(1L);
		room.setAvailable(true);
		room.setTimesBooked(0);
		room.setPricePerNight(100.0);
		return room;
	}
}