package com.n1str.booking.repository;

import com.n1str.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    List<Booking> findByUserId(Long userId);
    
    Optional<Booking> findByRequestId(String requestId);
    
    @Query("SELECT b FROM Booking b WHERE b.user.username = :username ORDER BY b.createdAt DESC")
    List<Booking> findByUsername(@Param("username") String username);
}

