package com.n1str.hotel.integration;

import com.n1str.hotel.entity.Hotel;
import com.n1str.hotel.entity.Room;
import com.n1str.hotel.repository.HotelRepository;
import com.n1str.hotel.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class HotelIntegrationTest {

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Test
    void testHotelCreationAndRetrieval() {
        // Create hotel
        Hotel hotel = new Hotel();
        hotel.setName("Test Hotel");
        hotel.setAddress("Test Address");
        hotel.setDescription("Test Description");
        hotel.setStarRating(4);
        
        Hotel savedHotel = hotelRepository.save(hotel);
        
        assertNotNull(savedHotel.getId());
        assertEquals("Test Hotel", savedHotel.getName());
        
        // Retrieve hotel
        Hotel retrieved = hotelRepository.findById(savedHotel.getId()).orElse(null);
        assertNotNull(retrieved);
        assertEquals("Test Hotel", retrieved.getName());
    }

    @Test
    void testRoomCreationWithHotel() {
        // Create hotel
        Hotel hotel = new Hotel();
        hotel.setName("Test Hotel");
        hotel.setAddress("Test Address");
        hotelRepository.save(hotel);
        
        // Create room
        Room room = new Room();
        room.setHotel(hotel);
        room.setNumber("101");
        room.setAvailable(true);
        room.setTimesBooked(0);
        room.setRoomType("Standard");
        room.setPricePerNight(100.0);
        room.setCapacity(2);
        
        Room savedRoom = roomRepository.save(room);
        
        assertNotNull(savedRoom.getId());
        assertEquals("101", savedRoom.getNumber());
        assertEquals(0, savedRoom.getTimesBooked());
    }

    @Test
    void testGetRecommendedRooms_SortedByTimesBooked() {
        // Create hotel
        Hotel hotel = new Hotel();
        hotel.setName("Test Hotel");
        hotel.setAddress("Test Address");
        hotelRepository.save(hotel);
        
        // Create rooms with different times_booked
        Room room1 = createRoom(hotel, "101", 10);
        Room room2 = createRoom(hotel, "102", 5);
        Room room3 = createRoom(hotel, "103", 2);
        
        roomRepository.saveAll(List.of(room1, room2, room3));
        
        // Get recommended rooms
        List<Room> recommended = roomRepository.findAllAvailableOrderByTimesBooked();
        
        assertEquals(3, recommended.size());
        assertEquals("103", recommended.get(0).getNumber()); // Least booked first
        assertEquals(2, recommended.get(0).getTimesBooked());
        assertEquals("102", recommended.get(1).getNumber());
        assertEquals(5, recommended.get(1).getTimesBooked());
    }

    private Room createRoom(Hotel hotel, String number, int timesBooked) {
        Room room = new Room();
        room.setHotel(hotel);
        room.setNumber(number);
        room.setAvailable(true);
        room.setTimesBooked(timesBooked);
        room.setRoomType("Standard");
        room.setPricePerNight(100.0);
        room.setCapacity(2);
        return room;
    }
}

