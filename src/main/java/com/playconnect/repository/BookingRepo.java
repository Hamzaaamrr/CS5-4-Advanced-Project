package com.playconnect.repository;

import com.playconnect.entity.Booking;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRepo extends JpaRepository<Booking, Long>{
    List<Booking> findByUserId(Long userId);
    Optional<Booking> findByIdAndUserId(Long id, Long userId);
}