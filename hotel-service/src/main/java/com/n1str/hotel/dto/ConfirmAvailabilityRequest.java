package com.n1str.hotel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmAvailabilityRequest {
    private LocalDate startDate;
    private LocalDate endDate;
    private Long bookingId;
    private String requestId;
}

