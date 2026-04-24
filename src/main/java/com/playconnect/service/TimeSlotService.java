package com.playconnect.service;

import org.springframework.beans.factory.annotation.Autowired;  // @Autowired = Spring automatically connects/injects the dependency
import org.springframework.stereotype.Service;  // @Service = Marks this class as a Service layer (holds business logic)

import com.playconnect.entity.TimeSlot;
import com.playconnect.repository.TimeSlotRepo;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;  // Optional = Container that may or may not hold a value (helps avoid null errors)

@Service  // @Service = This class handles TimeSlot business logic (Spring will detect it automatically)
public class TimeSlotService {
    
    @Autowired  // @Autowired = Spring automatically creates and injects TimeSlotRepo (no need for "new TimeSlotRepo()")
    private TimeSlotRepo timeSlotRepo;
    
    // Get all time slots for a specific court on a specific date
    public List<TimeSlot> getCourtSlotsByDate(Long courtId, LocalDate date) {
        return timeSlotRepo.findByCourtIdAndDateOrderByStartTimeAsc(courtId, date);
    }
    
    // Check if a specific time slot exists and is available
    public boolean isSlotAvailable(Long courtId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        Optional<TimeSlot> slot = timeSlotRepo.findByCourtIdAndDateAndStartTimeAndEndTime(
            courtId, date, startTime, endTime);
        
        return slot.isPresent() && slot.get().isAvailable();
    }
    
    // Get available slots only (not booked)
    public List<TimeSlot> getAvailableSlotsByDate(Long courtId, LocalDate date) {
        List<TimeSlot> allSlots = getCourtSlotsByDate(courtId, date);
        return allSlots.stream()
                .filter(TimeSlot::isAvailable)
                .toList();
    }
    
    // Mark a time slot as unavailable (when booked)
    public boolean markSlotAsUnavailable(Long slotId) {
        Optional<TimeSlot> slot = timeSlotRepo.findById(slotId);
        if (slot.isPresent()) {
            TimeSlot timeSlot = slot.get();
            timeSlot.setAvailable(false);
            timeSlotRepo.save(timeSlot);
            return true;
        }
        return false;
    }
    
    // Mark a time slot as available (when booking cancelled)
    public boolean markSlotAsAvailable(Long slotId) {
        Optional<TimeSlot> slot = timeSlotRepo.findById(slotId);
        if (slot.isPresent()) {
            TimeSlot timeSlot = slot.get();
            timeSlot.setAvailable(true);
            timeSlotRepo.save(timeSlot);
            return true;
        }
        return false;
    }
}
