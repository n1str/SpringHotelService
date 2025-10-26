package com.n1str.hotel.mapper;

import com.n1str.hotel.dto.CreateHotelRequest;
import com.n1str.hotel.dto.HotelDto;
import com.n1str.hotel.entity.Hotel;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface HotelMapper {
    
    HotelDto toDto(Hotel hotel);
    
    Hotel toEntity(CreateHotelRequest request);
}

