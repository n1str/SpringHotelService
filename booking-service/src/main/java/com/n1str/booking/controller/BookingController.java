package com.n1str.booking.controller;

import com.n1str.booking.dto.BookingDto;
import com.n1str.booking.dto.CreateBookingRequest;
import com.n1str.booking.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Bookings", description = "Booking management endpoints")
public class BookingController {

    private final BookingService bookingService;

    @PostMapping("/booking")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Create a new booking", 
               description = "Create a new booking with manual room selection or auto-selection")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<BookingDto> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            Authentication authentication) {
        
        String username = authentication.getName();
        log.info("User {} creating booking", username);
        
        BookingDto booking = bookingService.createBooking(username, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(booking);
    }

    @GetMapping("/bookings")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get user bookings", description = "Get booking history for the authenticated user")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<List<BookingDto>> getUserBookings(Authentication authentication) {
        String username = authentication.getName();
        log.debug("User {} fetching their bookings", username);
        
        List<BookingDto> bookings = bookingService.getUserBookings(username);
        return ResponseEntity.ok(bookings);
    }

    @GetMapping("/booking/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get booking by ID", description = "Get detailed information about a specific booking")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<BookingDto> getBookingById(
            @PathVariable Long id,
            Authentication authentication) {
        
        String username = authentication.getName();
        log.debug("User {} fetching booking {}", username, id);
        
        BookingDto booking = bookingService.getBookingById(username, id);
        return ResponseEntity.ok(booking);
    }

    @DeleteMapping("/booking/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Cancel booking", description = "Cancel an existing booking")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<Void> cancelBooking(
            @PathVariable Long id,
            Authentication authentication) {
        
        String username = authentication.getName();
        log.info("User {} cancelling booking {}", username, id);
        
        bookingService.cancelBooking(username, id);
        return ResponseEntity.noContent().build();
    }
}

