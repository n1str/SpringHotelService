package com.n1str.hotel.controller;

import com.n1str.hotel.dto.CreateHotelRequest;
import com.n1str.hotel.dto.HotelDto;
import com.n1str.hotel.service.HotelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hotels")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Hotels", description = "Hotel management endpoints")
public class HotelController {

    private final HotelService hotelService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new hotel", description = "Admin only - Create a new hotel in the system")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<HotelDto> createHotel(@Valid @RequestBody CreateHotelRequest request) {
        log.info("Received request to create hotel: {}", request.getName());
        HotelDto hotel = hotelService.createHotel(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(hotel);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get all hotels", description = "Get a list of all hotels")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<List<HotelDto>> getAllHotels() {
        log.debug("Received request to get all hotels");
        List<HotelDto> hotels = hotelService.getAllHotels();
        return ResponseEntity.ok(hotels);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get hotel by ID", description = "Get detailed information about a specific hotel")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<HotelDto> getHotelById(@PathVariable Long id) {
        log.debug("Received request to get hotel by ID: {}", id);
        HotelDto hotel = hotelService.getHotelById(id);
        return ResponseEntity.ok(hotel);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update hotel", description = "Admin only - Update an existing hotel")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<HotelDto> updateHotel(@PathVariable Long id, @Valid @RequestBody CreateHotelRequest request) {
        log.info("Received request to update hotel with ID: {}", id);
        HotelDto hotel = hotelService.updateHotel(id, request);
        return ResponseEntity.ok(hotel);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete hotel", description = "Admin only - Delete a hotel")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<Void> deleteHotel(@PathVariable Long id) {
        log.info("Received request to delete hotel with ID: {}", id);
        hotelService.deleteHotel(id);
        return ResponseEntity.noContent().build();
    }
}

