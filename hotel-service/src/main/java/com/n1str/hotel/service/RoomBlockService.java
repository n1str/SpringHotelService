package com.n1str.hotel.service;

import com.n1str.hotel.dto.ConfirmAvailabilityRequest;
import com.n1str.hotel.dto.ReleaseRoomRequest;
import com.n1str.hotel.entity.Room;
import com.n1str.hotel.entity.RoomBlock;
import com.n1str.hotel.repository.RoomBlockRepository;
import com.n1str.hotel.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomBlockService {

    private final RoomBlockRepository roomBlockRepository;
    private final RoomRepository roomRepository;
    private final RoomService roomService;
    
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public void confirmAvailability(Long roomId, ConfirmAvailabilityRequest request) {
        String traceId = request.getRequestId();
        log.info("[traceId:{}] Подтверждаем доступность номера {} с {} по {}", 
                traceId, roomId, request.getStartDate(), request.getEndDate());

        // ШАГ 1: Проверка идемпотентности - выходим, если уже обработано
        Optional<RoomBlock> existingBlock = roomBlockRepository.findByRequestId(request.getRequestId());
        if (existingBlock.isPresent()) {
            RoomBlock block = existingBlock.get();
            log.info("[traceId:{}] Запрос уже обработан со статусом: {}", traceId, block.getStatus());
            
            if ("CONFIRMED".equals(block.getStatus())) {
                log.info("[traceId:{}] Блок уже подтверждён, выходим", traceId);
                return;
            } else if ("PENDING".equals(block.getStatus())) {
                // Блок PENDING существует - переводим его в CONFIRMED
                block.setStatus("CONFIRMED");
                roomBlockRepository.save(block);
                log.info("[traceId:{}] Блок PENDING переведён в CONFIRMED", traceId);
                return;
            }
        }

        // ШАГ 2: Проверяем, существует ли номер и доступен ли он
        Room room = roomService.getRoomEntityById(roomId);
        if (!room.getAvailable()) {
            log.warn("[traceId:{}] Номер {} недоступен для бронирования", traceId, roomId);
            throw new RuntimeException("Номер недоступен для бронирования");
        }

        // ШАГ 3: Ищем конфликты с уже существующими подтверждёнными блоками
        List<RoomBlock> conflictingBlocks = roomBlockRepository.findConflictingBlocks(
                roomId, request.getStartDate(), request.getEndDate());
        
        boolean hasConfirmedConflict = conflictingBlocks.stream()
                .anyMatch(b -> "CONFIRMED".equals(b.getStatus()));
        
        if (hasConfirmedConflict) {
            log.warn("[traceId:{}] Номер {} уже забронирован на выбранные даты", traceId, roomId);
            throw new RuntimeException("Номер уже забронирован на выбранные даты");
        }

        // ШАГ 4: Создаём PENDING блок (HOLD фаза)
        RoomBlock block = new RoomBlock();
        block.setRoom(room);
        block.setStartDate(request.getStartDate());
        block.setEndDate(request.getEndDate());
        block.setBookingId(request.getBookingId());
        block.setRequestId(request.getRequestId());
        block.setStatus("PENDING");
        block.setCreatedAt(LocalDateTime.now());
        // TTL отсутствует - сохранено для обратной совместимости (null)
        block.setExpiresAt(null);
        
        roomBlockRepository.save(block);
        log.info("[traceId:{}] HOLD создан (блок PENDING) для бронирования {}", traceId, request.getBookingId());
    }

    @Transactional
    public void releaseRoom(Long roomId, ReleaseRoomRequest request) {
        String traceId = request.getRequestId();
        log.info("[traceId:{}] Освобождаем номер {} для запроса {}", traceId, roomId, request.getRequestId());

        Optional<RoomBlock> blockOpt = roomBlockRepository.findByRequestId(request.getRequestId());
        
        if (blockOpt.isEmpty()) {
            log.warn("[traceId:{}] Блок не найден, вероятно уже был освобождён", traceId);
            return; // Идемпотентный результат: если блока нет, он уже освобождён
        }

        RoomBlock block = blockOpt.get();
        
        // Уменьшаем счётчик только если блок был CONFIRMED
        if ("CONFIRMED".equals(block.getStatus())) {
            Room room = block.getRoom();
            if (room.getTimesBooked() > 0) {
                room.setTimesBooked(room.getTimesBooked() - 1);
                roomRepository.save(room);
                log.info("[traceId:{}] Счётчик times_booked номера {} уменьшен до {}", 
                        traceId, roomId, room.getTimesBooked());
            }
        }
        
        roomBlockRepository.delete(block);
        log.info("[traceId:{}] Номер успешно освобождён (блок удалён)", traceId);
    }
}

