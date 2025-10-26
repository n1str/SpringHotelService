package com.n1str.hotel.service;

import com.n1str.hotel.dto.CreateHotelRequest;
import com.n1str.hotel.dto.HotelDto;
import com.n1str.hotel.entity.Hotel;
import com.n1str.hotel.exception.EntityNotFoundException;
import com.n1str.hotel.mapper.HotelMapper;
import com.n1str.hotel.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HotelService {

    private final HotelRepository hotelRepository;
    private final HotelMapper hotelMapper;

    @Transactional
    public HotelDto createHotel(CreateHotelRequest request) {
        log.info("Creating new hotel: {}", request.getName());
        
        Hotel hotel = hotelMapper.toEntity(request);
        Hotel savedHotel = hotelRepository.save(hotel);
        
        log.info("Hotel created successfully with ID: {}", savedHotel.getId());
        return hotelMapper.toDto(savedHotel);
    }

    @Transactional(readOnly = true)
    public List<HotelDto> getAllHotels() {
        log.debug("Fetching all hotels");
        return hotelRepository.findAll().stream()
                .map(hotelMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public HotelDto getHotelById(Long id) {
        log.debug("Fetching hotel by ID: {}", id);
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Hotel not found with id: " + id));
        return hotelMapper.toDto(hotel);
    }

    @Transactional(readOnly = true)
    public Hotel getHotelEntityById(Long id) {
        return hotelRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Hotel not found with id: " + id));
    }

    @Transactional
    public HotelDto updateHotel(Long id, CreateHotelRequest request) {
        log.info("Updating hotel with ID: {}", id);
        Hotel hotel = getHotelEntityById(id);
        
        if (request.getName() != null) {
            hotel.setName(request.getName());
        }
        if (request.getAddress() != null) {
            hotel.setAddress(request.getAddress());
        }
        if (request.getDescription() != null) {
            hotel.setDescription(request.getDescription());
        }
        if (request.getStarRating() != null) {
            hotel.setStarRating(request.getStarRating());
        }
        
        Hotel updatedHotel = hotelRepository.save(hotel);
        log.info("Hotel updated successfully with ID: {}", id);
        return hotelMapper.toDto(updatedHotel);
    }

    @Transactional
    public void deleteHotel(Long id) {
        log.info("Deleting hotel with ID: {}", id);
        if (!hotelRepository.existsById(id)) {
            throw new EntityNotFoundException("Hotel not found with id: " + id);
        }
        hotelRepository.deleteById(id);
        log.info("Hotel deleted successfully with ID: {}", id);
    }
}

