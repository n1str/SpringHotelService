package com.n1str.hotel.controller;

import com.n1str.hotel.dto.ConfirmAvailabilityRequest;
import com.n1str.hotel.dto.CreateRoomRequest;
import com.n1str.hotel.dto.ReleaseRoomRequest;
import com.n1str.hotel.dto.RoomDto;
import com.n1str.hotel.service.RoomBlockService;
import com.n1str.hotel.service.RoomService;
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
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Rooms", description = "Room management endpoints")
public class RoomController {

    private final RoomService roomService;
    private final RoomBlockService roomBlockService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new room", description = "Admin only - Add a new room to a hotel")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<RoomDto> createRoom(@Valid @RequestBody CreateRoomRequest request) {
        log.info("Received request to create room {} for hotel {}", request.getNumber(), request.getHotelId());
        RoomDto room = roomService.createRoom(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(room);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get all available rooms", description = "Get a list of all available rooms")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<List<RoomDto>> getAllAvailableRooms() {
        log.debug("Received request to get all available rooms");
        List<RoomDto> rooms = roomService.getAllAvailableRooms();
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/recommend")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get recommended rooms", description = "Get available rooms sorted by booking frequency (least booked first)")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<List<RoomDto>> getRecommendedRooms() {
        log.debug("Received request to get recommended rooms");
        List<RoomDto> rooms = roomService.getRecommendedRooms();
        return ResponseEntity.ok(rooms);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    @Operation(summary = "Get room by ID", description = "Get detailed information about a specific room")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<RoomDto> getRoomById(@PathVariable Long id) {
        log.debug("Received request to get room by ID: {}", id);
        RoomDto room = roomService.getRoomById(id);
        return ResponseEntity.ok(room);
    }

    @PostMapping("/{id}/confirm-availability")
    @Operation(summary = "Confirm room availability", description = "Internal endpoint - Confirm and block room for booking")
    public ResponseEntity<Void> confirmAvailability(
            @PathVariable Long id,
            @Valid @RequestBody ConfirmAvailabilityRequest request) {
        log.info("Received request to confirm availability for room {} with requestId {}", id, request.getRequestId());
        try {
            roomBlockService.confirmAvailability(id, request);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            log.error("Failed to confirm availability: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PostMapping("/{id}/release")
    @Operation(summary = "Release room block", description = "Internal endpoint - Release a room block (compensation)")
    public ResponseEntity<Void> releaseRoom(
            @PathVariable Long id,
            @Valid @RequestBody ReleaseRoomRequest request) {
        log.info("Received request to release room {} with requestId {}", id, request.getRequestId());
        try {
            roomBlockService.releaseRoom(id, request);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            log.error("Failed to release room: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{id}/increment-booking")
    @Operation(summary = "Increment booking counter", description = "Internal endpoint - Increment room booking counter for algorithm")
    public ResponseEntity<Void> incrementTimesBooked(@PathVariable Long id) {
        log.info("Получили запрос на увеличение счётчика бронирований для номера {}", id);
        try {
            roomService.incrementTimesBooked(id);
            log.info("Счётчик бронирований для номера {} успешно увеличен", id);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            log.error("Ошибка при увеличении счётчика для номера {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update room", description = "Admin only - Update an existing room")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<RoomDto> updateRoom(@PathVariable Long id, @Valid @RequestBody CreateRoomRequest request) {
        log.info("Received request to update room with ID: {}", id);
        RoomDto room = roomService.updateRoom(id, request);
        return ResponseEntity.ok(room);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete room", description = "Admin only - Delete a room")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<Void> deleteRoom(@PathVariable Long id) {
        log.info("Received request to delete room with ID: {}", id);
        roomService.deleteRoom(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/stats/popular")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get popular rooms", description = "Admin only - Get rooms sorted by booking popularity (most booked first)")
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<List<RoomDto>> getPopularRooms() {
        log.debug("Received request to get popular rooms");
        List<RoomDto> rooms = roomService.getPopularRooms();
        return ResponseEntity.ok(rooms);
    }
}

