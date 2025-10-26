package com.n1str.hotel.service;

import com.n1str.hotel.dto.ConfirmAvailabilityRequest;
import com.n1str.hotel.entity.Hotel;
import com.n1str.hotel.entity.Room;
import com.n1str.hotel.entity.RoomBlock;
import com.n1str.hotel.repository.RoomBlockRepository;
import com.n1str.hotel.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomBlockServiceTest {

    @Mock
    private RoomBlockRepository roomBlockRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomService roomService;

    @InjectMocks
    private RoomBlockService roomBlockService;

    private Room testRoom;
    private ConfirmAvailabilityRequest testRequest;

    @BeforeEach
    void setUp() {
        Hotel hotel = new Hotel();
        hotel.setId(1L);
        hotel.setName("Test Hotel");

        testRoom = new Room();
        testRoom.setId(1L);
        testRoom.setHotel(hotel);
        testRoom.setAvailable(true);
        testRoom.setTimesBooked(0);

        testRequest = new ConfirmAvailabilityRequest();
        testRequest.setStartDate(LocalDate.now().plusDays(1));
        testRequest.setEndDate(LocalDate.now().plusDays(2));
        testRequest.setBookingId(1L);
        testRequest.setRequestId("test-request-123");
    }

    @Test
    void confirmAvailability_ShouldCreatePendingBlock_WhenNoConflicts() {
        when(roomService.getRoomEntityById(1L)).thenReturn(testRoom);
        when(roomBlockRepository.findByRequestId("test-request-123")).thenReturn(Optional.empty());
        when(roomBlockRepository.findConflictingBlocks(eq(1L), any(), any())).thenReturn(Collections.emptyList());

        assertDoesNotThrow(() -> roomBlockService.confirmAvailability(1L, testRequest));

        verify(roomBlockRepository).save(any(RoomBlock.class));
    }

    @Test
    void confirmAvailability_ShouldReturn_WhenRequestAlreadyProcessed() {
        RoomBlock existingBlock = new RoomBlock();
        existingBlock.setStatus("CONFIRMED");
        when(roomBlockRepository.findByRequestId("test-request-123")).thenReturn(Optional.of(existingBlock));

        assertDoesNotThrow(() -> roomBlockService.confirmAvailability(1L, testRequest));

        verify(roomBlockRepository, never()).save(any());
    }

    @Test
    void confirmAvailability_ShouldThrowException_WhenRoomNotAvailable() {
        testRoom.setAvailable(false);
        when(roomService.getRoomEntityById(1L)).thenReturn(testRoom);
        when(roomBlockRepository.findByRequestId("test-request-123")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> roomBlockService.confirmAvailability(1L, testRequest));
        assertEquals("Номер недоступен для бронирования", exception.getMessage());
    }

    @Test
    void confirmAvailability_ShouldThrowException_WhenConfirmedConflictExists() {
        when(roomService.getRoomEntityById(1L)).thenReturn(testRoom);
        when(roomBlockRepository.findByRequestId("test-request-123")).thenReturn(Optional.empty());

        RoomBlock confirmedBlock = new RoomBlock();
        confirmedBlock.setStatus("CONFIRMED");
        when(roomBlockRepository.findConflictingBlocks(eq(1L), any(), any()))
                .thenReturn(Collections.singletonList(confirmedBlock));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> roomBlockService.confirmAvailability(1L, testRequest));
        assertEquals("Номер уже забронирован на выбранные даты", exception.getMessage());
    }

    @Test
    void releaseRoom_ShouldBeIdempotent_WhenBlockNotFound() {
        when(roomBlockRepository.findByRequestId("test-request-123")).thenReturn(Optional.empty());

        com.n1str.hotel.dto.ReleaseRoomRequest releaseRequest = new com.n1str.hotel.dto.ReleaseRoomRequest();
        releaseRequest.setRequestId("test-request-123");
        assertDoesNotThrow(() -> roomBlockService.releaseRoom(1L, releaseRequest));

        verify(roomBlockRepository, never()).delete(any());
    }
}