package com.playconnect.repository;

import com.playconnect.entity.TimeSlot;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface TimeSlotRepo extends JpaRepository<TimeSlot, Long>{
    List<TimeSlot> findByCourtIdAndDateOrderByStartTimeAsc(Long courtId, LocalDate date);
    Optional<TimeSlot> findByCourtIdAndDateAndStartTimeAndEndTime(Long courtId, LocalDate date, LocalTime startTime, LocalTime endTime);
}