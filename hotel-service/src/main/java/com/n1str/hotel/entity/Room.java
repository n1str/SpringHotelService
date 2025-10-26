package com.n1str.hotel.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rooms")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;

    @Column(nullable = false)
    private String number;

    @Column(nullable = false)
    private Boolean available = true;

    @Column(name = "times_booked", nullable = false)
    private Integer timesBooked = 0;

    @Column(name = "room_type")
    private String roomType;

    @Column(name = "price_per_night")
    private Double pricePerNight;

    @Column
    private Integer capacity;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoomBlock> blocks = new ArrayList<>();

    @Version
    private Long version = 0L;
}

