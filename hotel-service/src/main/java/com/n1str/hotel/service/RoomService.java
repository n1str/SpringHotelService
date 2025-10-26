package com.n1str.hotel.service;

import com.n1str.hotel.dto.CreateRoomRequest;
import com.n1str.hotel.dto.RoomDto;
import com.n1str.hotel.entity.Hotel;
import com.n1str.hotel.entity.Room;
import com.n1str.hotel.exception.EntityNotFoundException;
import com.n1str.hotel.mapper.RoomMapper;
import com.n1str.hotel.repository.RoomBlockRepository;
import com.n1str.hotel.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomBlockRepository roomBlockRepository;
    private final RoomMapper roomMapper;
    private final HotelService hotelService;
    
    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public RoomDto createRoom(CreateRoomRequest request) {
        log.info("Creating new room {} for hotel {}", request.getNumber(), request.getHotelId());
        
        Hotel hotel = hotelService.getHotelEntityById(request.getHotelId());
        
        Room room = roomMapper.toEntity(request);
        room.setHotel(hotel);
        
        Room savedRoom = roomRepository.save(room);
        
        log.info("Room created successfully with ID: {}", savedRoom.getId());
        return roomMapper.toDto(savedRoom);
    }

    @Transactional(readOnly = true)
    public List<RoomDto> getAllAvailableRooms() {
        log.debug("Fetching all available rooms");
        return roomRepository.findAllAvailable().stream()
                .filter(this::isRoomAvailableNow)
                .map(roomMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<RoomDto> getRecommendedRooms() {
        log.debug("Fetching recommended rooms sorted by times_booked");
        return roomRepository.findAllAvailableOrderByTimesBooked().stream()
                .sorted(java.util.Comparator
                        .comparing(Room::getTimesBooked)
                        .thenComparing(Room::getId))
                .filter(this::isRoomAvailableNow)
                .map(roomMapper::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RoomDto getRoomById(Long id) {
        log.debug("Fetching room by ID: {}", id);
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Room not found with id: " + id));
        return roomMapper.toDto(room);
    }

    @Transactional(readOnly = true)
    public Room getRoomEntityById(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Room not found with id: " + id));
    }

    private boolean isRoomAvailableNow(Room room) {
        if (!room.getAvailable()) {
            return false;
        }
        
        // Проверяем, есть ли активные подтверждённые блокировки на сегодня
        LocalDate today = LocalDate.now();
        long activeBlocks = roomBlockRepository.findConflictingBlocks(
                room.getId(), today, today).size();
        
        return activeBlocks == 0;
    }

    @Transactional
    public void incrementTimesBooked(Long roomId) {
        log.debug("Incrementing times_booked for room ID: {}", roomId);
        Room room = getRoomEntityById(roomId);
        room.setTimesBooked(room.getTimesBooked() + 1);
        roomRepository.save(room);
    }

    @Transactional
    public RoomDto updateRoom(Long id, CreateRoomRequest request) {
        log.info("Обновляем номер с ID: {}", id);
        
        try {
            // Получаем комнату из базы данных
            Room room = roomRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Комната не найдена с id: " + id));
            
            log.debug("Текущие данные комнаты до обновления: number={}, type={}, price={}, version={}", 
                     room.getNumber(), room.getRoomType(), room.getPricePerNight(), room.getVersion());
            
            // Убедимся что версия не null
            if (room.getVersion() == null) {
                room.setVersion(0L);
            }
            
            // Обновляем отель если указан и отличается от текущего
            if (request.getHotelId() != null) {
                Hotel currentHotel = room.getHotel();
                Long currentHotelId = currentHotel != null ? currentHotel.getId() : null;
                
                if (currentHotelId == null || !currentHotelId.equals(request.getHotelId())) {
                    log.info("Меняем отель комнаты {} с {} на {}", id, currentHotelId, request.getHotelId());
                    Hotel newHotel = hotelService.getHotelEntityById(request.getHotelId());
                    room.setHotel(newHotel);
                }
            }
            
            // Обновляем поля комнаты
            if (request.getNumber() != null) {
                log.debug("Обновляем номер с '{}' на '{}'", room.getNumber(), request.getNumber());
                room.setNumber(request.getNumber());
            }
            if (request.getAvailable() != null) {
                log.debug("Обновляем доступность с {} на {}", room.getAvailable(), request.getAvailable());
                room.setAvailable(request.getAvailable());
            }
            if (request.getRoomType() != null) {
                log.debug("Обновляем тип с '{}' на '{}'", room.getRoomType(), request.getRoomType());
                room.setRoomType(request.getRoomType());
            }
            if (request.getPricePerNight() != null) {
                log.debug("Обновляем цену с {} на {}", room.getPricePerNight(), request.getPricePerNight());
                room.setPricePerNight(request.getPricePerNight());
            }
            if (request.getCapacity() != null) {
                log.debug("Обновляем вместимость с {} на {}", room.getCapacity(), request.getCapacity());
                room.setCapacity(request.getCapacity());
            }
            
            // Сохраняем изменения
            Room updatedRoom = roomRepository.save(room);
            
            // Принудительно сбрасываем изменения в базу данных
            entityManager.flush();
            
            log.info("Номер успешно обновлён с ID: {}. Новые данные: number={}, type={}, price={}, version={}", 
                     id, updatedRoom.getNumber(), updatedRoom.getRoomType(), updatedRoom.getPricePerNight(), updatedRoom.getVersion());
            
            return roomMapper.toDto(updatedRoom);
            
        } catch (EntityNotFoundException e) {
            log.error("Комната с ID {} не найдена: {}", id, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при обновлении номера {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Не удалось обновить номер: " + e.getMessage(), e);
        }
    }

    @Transactional
    public void deleteRoom(Long id) {
        log.info("Удаляем номер с ID: {}", id);
        
        try {
            // Проверяем, существует ли комната
            Room room = roomRepository.findById(id)
                    .orElseThrow(() -> new EntityNotFoundException("Номер не найден с id: " + id));
            
            log.debug("Найдена комната для удаления: {}, отель ID: {}", 
                     room.getNumber(), room.getHotel().getId());
            
            // Сначала удаляем все блокировки комнаты (room_blocks)
            // чтобы избежать нарушения foreign key constraint
            log.debug("Удаляем блокировки для номера {}", id);
            roomBlockRepository.deleteAllByRoomId(id);
            
            // Принудительно сбрасываем удаление блокировок
            entityManager.flush();
            
            log.info("Удалены все блокировки для номера с ID: {}", id);
            
            // Теперь удаляем саму комнату
            log.debug("Удаляем саму комнату {}", id);
            roomRepository.deleteById(id);
            
            // Принудительно сбрасываем удаление комнаты  
            entityManager.flush();
            
            log.info("Номер успешно удалён с ID: {}", id);
            
        } catch (EntityNotFoundException e) {
            log.error("Комната с ID {} не найдена для удаления: {}", id, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Ошибка при удалении номера {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Не удалось удалить номер: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<RoomDto> getPopularRooms() {
        log.debug("Fetching popular rooms sorted by times_booked (descending)");
        return roomRepository.findAll().stream()
                .sorted(java.util.Comparator
                        .comparing(Room::getTimesBooked).reversed()
                        .thenComparing(Room::getId))
                .map(roomMapper::toDto)
                .collect(Collectors.toList());
    }
}

