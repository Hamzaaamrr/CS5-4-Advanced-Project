package com.playconnect.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.playconnect.entity.TimeSlot;

@Repository
public interface TimeSlotRepo extends JpaRepository<TimeSlot, Long>{
    Optional<TimeSlot> findByCourtIdAndDateAndStartTimeAndEndTime(Long courtId, LocalDate date, LocalTime startTime, LocalTime endTime);
    List<TimeSlot> findByCourtIdAndDateAndStartTimeLessThanAndEndTimeGreaterThan(Long courtId, LocalDate date, LocalTime endTime, LocalTime startTime);
}
