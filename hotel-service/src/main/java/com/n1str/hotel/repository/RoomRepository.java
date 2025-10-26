package com.n1str.hotel.repository;

import com.n1str.hotel.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    @Query("SELECT r FROM Room r WHERE r.available = true")
    List<Room> findAllAvailable();

    @Query("SELECT r FROM Room r WHERE r.available = true ORDER BY r.timesBooked ASC, r.id ASC")
    List<Room> findAllAvailableOrderByTimesBooked();

    @Query("SELECT r FROM Room r WHERE r.hotel.id = :hotelId AND r.available = true")
    List<Room> findAvailableByHotelId(@Param("hotelId") Long hotelId);

    @Modifying
    @Query("UPDATE Room r SET r.timesBooked = 0")
    void resetAllBookingCounters();
}

