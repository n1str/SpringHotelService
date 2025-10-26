package com.n1str.booking.service;

import com.n1str.booking.client.HotelServiceClient;
import com.n1str.booking.dto.BookingDto;
import com.n1str.booking.dto.CreateBookingRequest;
import com.n1str.booking.dto.RoomDto;
import com.n1str.booking.entity.Booking;
import com.n1str.booking.entity.User;
import com.n1str.booking.repository.BookingRepository;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final UserService userService;
    private final HotelServiceClient hotelServiceClient;

    @Transactional
    public BookingDto createBooking(String username, CreateBookingRequest request) {
        String requestId = UUID.randomUUID().toString();
        log.info("[traceId:{}] Создаём бронирование для пользователя: {}", requestId, username);

        // Проверяем валидность дат
        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Нельзя забронировать в прошлое");
        }
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("Дата начала должна быть раньше даты конца");
        }

        User user = userService.getUserByUsername(username);

        // Шаг 1: Определяем какой номер будет забронирован
        Long roomId;
        if (request.getAutoSelect()) {
            log.info("[traceId:{}] Автоматически подбираем номер по алгоритму рекомендаций", requestId);
            roomId = selectBestRoom(requestId);
        } else {
            if (request.getRoomId() == null) {
                throw new IllegalArgumentException("ID номера обязателен когда autoSelect=false");
            }
            roomId = request.getRoomId();
        }

        log.info("[traceId:{}] Выбран номер ID: {}", requestId, roomId);

        // Получаем информацию о номере для расчёта цены
        RoomDto room = null;
        try {
            room = hotelServiceClient.getRoomById(roomId);
        } catch (Exception e) {
            log.error("[traceId:{}] Не удалось получить данные номера из Hotel Service: {}", requestId, e.getMessage(), e);
            throw new RuntimeException("Hotel Service недоступен - не могу получить данные номера", e);
        }
        
        if (room == null) {
            log.error("[traceId:{}] Hotel Service вернул null для номера", requestId);
            throw new RuntimeException("Полученные от Hotel Service данные некорректны");
        }
        
        long nights = ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
        double totalPrice = room.getPricePerNight() * nights;

        // Шаг 2: Создаём бронирование со статусом PENDING
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setRoomId(roomId);
        booking.setHotelId(room.getHotelId());
        booking.setStartDate(request.getStartDate());
        booking.setEndDate(request.getEndDate());
        booking.setStatus("PENDING");
        booking.setRequestId(requestId);
        booking.setCreatedAt(LocalDateTime.now());
        booking.setTotalPrice(totalPrice);

        booking = bookingRepository.save(booking);
        log.info("[traceId:{}] Бронирование создано со статусом PENDING, ID: {}", requestId, booking.getId());

        // Шаг 3: Подтверждаем доступность у Hotel Service (с повторными попытками)
        try {
            confirmRoomAvailability(roomId, booking.getId(), request.getStartDate(), request.getEndDate(), requestId);
            
            // Шаг 4: Переводим бронирование в статус CONFIRMED
            booking.setStatus("CONFIRMED");
            booking.setUpdatedAt(LocalDateTime.now());
            booking = bookingRepository.save(booking);

            // Шаг 5: Обновляем статистику популярности номера
            try {
                hotelServiceClient.incrementTimesBooked(roomId);
                log.info("[traceId:{}] Счётчик бронирований номера {} обновлён", requestId, roomId);
            } catch (Exception e) {
                log.warn("[traceId:{}] Не удалось обновить счётчик: {}", requestId, e.getMessage());
                // Продолжаем - бронирование успешно
            }

            log.info("[traceId:{}] Бронирование подтверждено, ID: {}", requestId, booking.getId());
            return toDto(booking);
               
        } catch (FeignException fe) {
            log.error("[traceId:{}] Ошибка от hotel service: {} - Статус: {}", requestId, fe.getMessage(), fe.status());
            
            // Шаг 5: Компенсация - отменяем бронирование
            booking.setStatus("CANCELLED");
            booking.setUpdatedAt(LocalDateTime.now());
            bookingRepository.save(booking);
            
            // Пробрасываем исключение чтобы GlobalExceptionHandler его обработал
            throw fe;
            
        } catch (Exception e) {
            log.error("[traceId:{}] Не удалось подтвердить бронирование: {}", requestId, e.getMessage());
            
            // Шаг 5: Компенсация - отменяем бронирование
            booking.setStatus("CANCELLED");
            booking.setUpdatedAt(LocalDateTime.now());
            bookingRepository.save(booking);
            
            throw new RuntimeException("Не удалось подтвердить бронирование: " + e.getMessage(), e);
        }
    }

    @Retryable(
        retryFor = { FeignException.class, RuntimeException.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @CircuitBreaker(name = "hotelService", fallbackMethod = "fallbackConfirmAvailability")
    public void confirmRoomAvailability(Long roomId, Long bookingId, LocalDate startDate, LocalDate endDate, String requestId) {
        log.info("[traceId:{}] Пытаемся подтвердить доступность номера (с повторами и circuit breaker)", requestId);
        
        Map<String, Object> confirmRequest = new HashMap<>();
        confirmRequest.put("startDate", startDate.toString());
        confirmRequest.put("endDate", endDate.toString());
        confirmRequest.put("bookingId", bookingId);
        confirmRequest.put("requestId", requestId);
        
        hotelServiceClient.confirmAvailability(roomId, confirmRequest);
        log.info("[traceId:{}] Доступность номера подтверждена", requestId);
    }

    public void fallbackConfirmAvailability(Long roomId, Long bookingId, LocalDate startDate, LocalDate endDate, String requestId, Exception e) {
        log.error("[traceId:{}] Circuit breaker открыт или fallback сработал: {}", requestId, e.getMessage());
        throw new RuntimeException("Hotel Service недоступен (circuit breaker открыт). Бронирование отменено.", e);
    }

    @Recover
    public void recoverFromConfirmFailure(Exception e, Long roomId, Long bookingId, LocalDate startDate, LocalDate endDate, String requestId) {
        log.error("[traceId:{}] Все попытки повтора исчерпаны при подтверждении доступности номера", requestId);
        throw new RuntimeException("Не удалось подтвердить доступность номера после всех повторов", e);
    }

    private void releaseRoomBlock(Long roomId, String requestId) {
        log.info("[traceId:{}] Освобождаем блок номера (компенсация)", requestId);
        
        Map<String, String> releaseRequest = new HashMap<>();
        releaseRequest.put("requestId", requestId);
        
        hotelServiceClient.releaseRoom(roomId, releaseRequest);
    }

    private Long selectBestRoom(String traceId) {
        log.debug("[traceId:{}] Получаем рекомендованные номера", traceId);
        
        List<RoomDto> rooms = hotelServiceClient.getRecommendedRooms();
        
        if (rooms.isEmpty()) {
            throw new RuntimeException("Нет доступных номеров");
        }
        
        // Алгоритм: выбираем номер с минимальным счётчиком times_booked (уже отсортировано Hotel Service)
        RoomDto selectedRoom = rooms.get(0);
        log.info("[traceId:{}] Выбран номер {} с timesBooked: {}", 
                traceId, selectedRoom.getId(), selectedRoom.getTimesBooked());
        
        return selectedRoom.getId();
    }

    @Transactional(readOnly = true)
    public List<BookingDto> getUserBookings(String username) {
        log.debug("Fetching bookings for user: {}", username);
        
        List<Booking> bookings = bookingRepository.findByUsername(username);
        return bookings.stream()
                .map(this::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public BookingDto getBookingById(String username, Long bookingId) {
        log.debug("Fetching booking {} for user: {}", bookingId, username);
        
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        if (!booking.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Access denied: This booking belongs to another user");
        }
        
        return toDto(booking);
    }

    @Transactional
    public void cancelBooking(String username, Long bookingId) {
        String requestId = UUID.randomUUID().toString();
        log.info("[traceId:{}] Отменяем бронирование {} для пользователя: {}", requestId, bookingId, username);
        
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Booking not found"));
        
        if (!booking.getUser().getUsername().equals(username)) {
            throw new RuntimeException("Access denied: This booking belongs to another user");
        }
        
        // Идемпотентный результат - если уже отменено, просто возвращаем
        if ("CANCELLED".equals(booking.getStatus())) {
            log.warn("[traceId:{}] Бронирование уже отменено, ничего не делаем", requestId);
            return;
        }
        
        // Освобождаем блок номера если бронирование было CONFIRMED
        if ("CONFIRMED".equals(booking.getStatus())) {
            try {
                releaseRoomBlock(booking.getRoomId(), booking.getRequestId());
            } catch (Exception e) {
                log.warn("[traceId:{}] Не удалось освободить блок номера: {}", requestId, e.getMessage());
            }
        }
        
        booking.setStatus("CANCELLED");
        booking.setUpdatedAt(LocalDateTime.now());
        bookingRepository.save(booking);
        
        log.info("[traceId:{}] Бронирование успешно отменено", requestId);
    }

    private BookingDto toDto(Booking booking) {
        BookingDto dto = new BookingDto();
        dto.setId(booking.getId());
        dto.setUserId(booking.getUser().getId());
        dto.setUsername(booking.getUser().getUsername());
        dto.setRoomId(booking.getRoomId());
        dto.setHotelId(booking.getHotelId());
        dto.setStartDate(booking.getStartDate());
        dto.setEndDate(booking.getEndDate());
        dto.setStatus(booking.getStatus());
        dto.setCreatedAt(booking.getCreatedAt());
        dto.setTotalPrice(booking.getTotalPrice());
        return dto;
    }
}

