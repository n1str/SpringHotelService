package com.n1str.hotel.service;

import com.n1str.hotel.dto.RoomDto;
import com.n1str.hotel.entity.Hotel;
import com.n1str.hotel.entity.Room;
import com.n1str.hotel.mapper.RoomMapper;
import com.n1str.hotel.repository.RoomBlockRepository;
import com.n1str.hotel.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomBlockRepository roomBlockRepository;

    @Mock
    private RoomMapper roomMapper;

    @Mock
    private HotelService hotelService;

    @InjectMocks
    private RoomService roomService;

    private Hotel testHotel;
    private List<Room> testRooms;

    @BeforeEach
    void setUp() {
        testHotel = new Hotel();
        testHotel.setId(1L);
        testHotel.setName("Test Hotel");

        // Create rooms with different timesBooked values for algorithm testing
        Room room1 = new Room();
        room1.setId(1L);
        room1.setHotel(testHotel);
        room1.setAvailable(true);
        room1.setTimesBooked(0);
        room1.setNumber("101");
        room1.setPricePerNight(100.0);

        Room room2 = new Room();
        room2.setId(2L);
        room2.setHotel(testHotel);
        room2.setAvailable(true);
        room2.setTimesBooked(5);
        room2.setNumber("102");
        room2.setPricePerNight(120.0);

        Room room3 = new Room();
        room3.setId(3L);
        room3.setHotel(testHotel);
        room3.setAvailable(true);
        room3.setTimesBooked(1);
        room3.setNumber("103");
        room3.setPricePerNight(110.0);

        testRooms = Arrays.asList(room1, room2, room3);
    }

    @Test
    void getRecommendedRooms_ShouldReturnRoomsSortedByTimesBooked() {
        // Given
        when(roomRepository.findAllAvailableOrderByTimesBooked()).thenReturn(testRooms);

        RoomDto roomDto1 = new RoomDto();
        roomDto1.setId(1L);
        roomDto1.setTimesBooked(0);
        RoomDto roomDto2 = new RoomDto();
        roomDto2.setId(2L);
        roomDto2.setTimesBooked(5);
        RoomDto roomDto3 = new RoomDto();
        roomDto3.setId(3L);
        roomDto3.setTimesBooked(1);

        when(roomMapper.toDto(any(Room.class))).thenAnswer(invocation -> {
            Room r = invocation.getArgument(0);
            RoomDto dto = new RoomDto();
            dto.setId(r.getId());
            dto.setTimesBooked(r.getTimesBooked());
            return dto;
        });

        // When
        List<RoomDto> result = roomService.getRecommendedRooms();

        // Then
        assertNotNull(result);
        assertEquals(3, result.size());

        // Verify algorithm: rooms should be sorted by timesBooked ascending
        assertEquals(0, result.get(0).getTimesBooked()); // Room 1 (0 bookings)
        assertEquals(1, result.get(1).getTimesBooked()); // Room 3 (1 booking)
        assertEquals(5, result.get(2).getTimesBooked()); // Room 2 (5 bookings)

        verify(roomRepository).findAllAvailableOrderByTimesBooked();
    }

    @Test
    void getRecommendedRooms_ShouldFilterUnavailableRooms() {
        // Given
        Room unavailableRoom = new Room();
        unavailableRoom.setId(4L);
        unavailableRoom.setAvailable(false);
        unavailableRoom.setTimesBooked(2);

        testRooms = Arrays.asList(testRooms.get(0), testRooms.get(1), unavailableRoom);

        when(roomRepository.findAllAvailableOrderByTimesBooked()).thenReturn(testRooms);
        when(roomMapper.toDto(any(Room.class))).thenReturn(new RoomDto());

        // When
        List<RoomDto> result = roomService.getRecommendedRooms();

        // Then
        assertEquals(2, result.size()); // Should exclude unavailable room
    }

    @Test
    void getRecommendedRooms_ShouldReturnEmptyList_WhenNoRoomsAvailable() {
        // Given
        when(roomRepository.findAllAvailableOrderByTimesBooked()).thenReturn(Collections.emptyList());

        // When
        List<RoomDto> result = roomService.getRecommendedRooms();

        // Then
        assertTrue(result.isEmpty());
    }

    @Test
    void incrementTimesBooked_ShouldUpdateCounter() {
        // Given
        Room room = testRooms.get(0);
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(roomRepository.save(any(Room.class))).thenReturn(room);

        // When
        roomService.incrementTimesBooked(1L);

        // Then
        assertEquals(1, room.getTimesBooked());
        verify(roomRepository).save(room);
    }
}
