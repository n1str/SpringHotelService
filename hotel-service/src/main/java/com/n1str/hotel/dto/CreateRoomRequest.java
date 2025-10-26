package com.n1str.hotel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoomRequest {
    
    @NotNull(message = "Hotel ID is required")
    private Long hotelId;
    
    @NotBlank(message = "Room number is required")
    private String number;
    
    private Boolean available = true;
    
    private String roomType;
    
    @Positive(message = "Price must be positive")
    private Double pricePerNight;
    
    @Positive(message = "Capacity must be positive")
    private Integer capacity;
}

