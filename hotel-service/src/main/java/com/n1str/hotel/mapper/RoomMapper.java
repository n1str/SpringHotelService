package com.n1str.hotel.mapper;

import com.n1str.hotel.dto.CreateRoomRequest;
import com.n1str.hotel.dto.RoomDto;
import com.n1str.hotel.entity.Room;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RoomMapper {
    
    @Mapping(source = "hotel.id", target = "hotelId")
    RoomDto toDto(Room room);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "hotel", ignore = true)
    @Mapping(target = "timesBooked", constant = "0")
    @Mapping(target = "blocks", ignore = true)
    @Mapping(target = "version", ignore = true)
    Room toEntity(CreateRoomRequest request);
}

