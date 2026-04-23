package com.playconnect.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.playconnect.entity.Booking;

@Repository
public interface BookingRepo extends JpaRepository<Booking, Long>{
}
