package com.playconnect.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.playconnect.entity.TimeSlot;
import com.playconnect.repository.TimeSlotRepo;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class TimeSlotService {
    
    @Autowired
    private TimeSlotRepo timeSlotRepo;
    
    public List<TimeSlot> getCourtSlotsByDate(Long courtId, LocalDate date) {
        return timeSlotRepo.findByCourtIdAndDateOrderByStartTimeAsc(courtId, date);
    }
    
    public boolean isSlotAvailable(Long courtId, LocalDate date, LocalTime startTime, LocalTime endTime) {
        Optional<TimeSlot> slot = timeSlotRepo.findByCourtIdAndDateAndStartTimeAndEndTime(
            courtId, date, startTime, endTime);
        
        return slot.isPresent() && slot.get().isAvailable();
    }
    
    public List<TimeSlot> getAvailableSlotsByDate(Long courtId, LocalDate date) {
        List<TimeSlot> allSlots = getCourtSlotsByDate(courtId, date);
        return allSlots.stream()
                .filter(TimeSlot::isAvailable)
                .toList();
    }
    
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