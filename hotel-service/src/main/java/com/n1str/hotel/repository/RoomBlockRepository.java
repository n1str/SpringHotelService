package com.n1str.hotel.repository;

import com.n1str.hotel.entity.RoomBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomBlockRepository extends JpaRepository<RoomBlock, Long> {

    Optional<RoomBlock> findByRequestId(String requestId);

    @Query("SELECT rb FROM RoomBlock rb WHERE rb.room.id = :roomId AND rb.status IN ('PENDING', 'CONFIRMED') " +
           "AND ((rb.startDate <= :endDate AND rb.endDate >= :startDate))")
    List<RoomBlock> findConflictingBlocks(@Param("roomId") Long roomId,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    @Query("SELECT rb FROM RoomBlock rb WHERE rb.status = 'PENDING' AND rb.expiresAt < :now")
    List<RoomBlock> findExpiredPendingBlocks(@Param("now") LocalDateTime now);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM RoomBlock rb WHERE rb.status = 'PENDING'")
    void deleteAllPendingBlocks();
    
    @Query("SELECT rb FROM RoomBlock rb WHERE rb.room.id = :roomId AND rb.status = 'PENDING' " +
           "AND ((rb.startDate <= :endDate AND rb.endDate >= :startDate))")
    List<RoomBlock> findPendingBlocksForDates(@Param("roomId") Long roomId,
                                              @Param("startDate") LocalDate startDate,
                                              @Param("endDate") LocalDate endDate);

    List<RoomBlock> findByBookingId(Long bookingId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM RoomBlock rb WHERE rb.room.id = :roomId")
    void deleteAllByRoomId(@Param("roomId") Long roomId);
}

