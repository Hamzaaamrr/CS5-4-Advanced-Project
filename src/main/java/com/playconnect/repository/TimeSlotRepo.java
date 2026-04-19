package com.playconnect.repository;

import com.playconnect.entity.TimeSlot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface TimeSlotRepo extends JpaRepository<TimeSlot, Long>{
    
}
