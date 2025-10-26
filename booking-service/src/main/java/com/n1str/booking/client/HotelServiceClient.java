package com.n1str.booking.client;

import com.n1str.booking.dto.RoomDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@FeignClient(name = "hotel-service")
public interface HotelServiceClient {

    @GetMapping("/api/rooms/recommend")
    List<RoomDto> getRecommendedRooms();

    @GetMapping("/api/rooms/{id}")
    RoomDto getRoomById(@PathVariable("id") Long id);

    @PostMapping("/api/rooms/{id}/confirm-availability")
    void confirmAvailability(
            @PathVariable("id") Long roomId,
            @RequestBody Map<String, Object> request);

    @PostMapping("/api/rooms/{id}/release")
    void releaseRoom(
            @PathVariable("id") Long roomId,
            @RequestBody Map<String, String> request);

    @PostMapping("/api/rooms/{id}/increment-booking")
    void incrementTimesBooked(@PathVariable("id") Long roomId);
}

