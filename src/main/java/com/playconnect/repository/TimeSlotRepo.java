package repository;  

import java.time.LocalDate; 
import java.time.LocalTime;  
import java.util.List;       // For returning multiple time slots
import java.util.Optional;   // For returning a time slot that might be empty (null safe)

import org.springframework.data.jpa.repository.JpaRepository;  // Gives built-in database methods (no SQL)
import org.springframework.stereotype.Repository;  // Marks this as a Repository component

import Entity.TimeSlot; 
//This file is in the repository folder (database operations)
//This is a Repository (Spring will create the implementation automatically)
@Repository  
public interface TimeSlotRepo extends JpaRepository<TimeSlot, Long> {  // <Entity type, ID type>
    
    // 1. Find all time slots for a specific court on a specific date, sorted by start time (earliest first)
    List<TimeSlot> findByCourtIdAndDateOrderByStartTimeAsc(Long courtId, LocalDate date);
    
    // 2. Find all time slots for a specific court (any date): for deletion when court is deleted
    List<TimeSlot> findByCourtId(Long courtId);
    
    // 3. Find ONE specific time slot by court, date, start time, and end time (used when booking)
    Optional<TimeSlot> findByCourtIdAndDateAndStartTimeAndEndTime(Long courtId, LocalDate date, LocalTime startTime, LocalTime endTime);
}
