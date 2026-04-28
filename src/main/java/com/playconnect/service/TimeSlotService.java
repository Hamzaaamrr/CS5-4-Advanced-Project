package com.playconnect.service;

import java.time.LocalDate;  // @Autowired = Spring automatically connects/injects the dependency
import java.time.LocalTime;  // @Service = Marks this class as a Service layer (holds business logic)
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.playconnect.entity.Court;
import com.playconnect.entity.TimeSlot;
import com.playconnect.repository.TimeSlotRepo;

import jakarta.transaction.Transactional;  // Optional = Container that may or may not hold a value (helps avoid null errors)

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

    // Get distinct available dates for a court
    public List<LocalDate> getAvailableDates(Long courtId) {
        List<TimeSlot> availableSlots = timeSlotRepo.findByCourtIdAndAvailableTrue(courtId);
        return availableSlots.stream()
                .map(TimeSlot::getDate)
                .distinct()
                .sorted()
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

    @Transactional
    public void generateSlots(Court court, LocalDate date,LocalTime startHour,LocalTime endHour) {

        LocalTime current = startHour;

        while (current.isBefore(endHour)) {

            LocalTime next = current.plusHours(1);

            Optional<TimeSlot> existing =
                    timeSlotRepo.findByCourtIdAndDateAndStartTimeAndEndTime(
                            court.getId(), date, current, next);

            if (existing.isEmpty()) {
                TimeSlot slot = new TimeSlot();
                slot.setCourt(court);
                slot.setDate(date);
                slot.setStartTime(current);
                slot.setEndTime(next);
                slot.setAvailable(true);

                timeSlotRepo.save(slot);
            }

            current = next;
        }
    }




    //****************Every court automatically has slots for the next 30 days
    @Transactional
    public void generateSlotsForNextDays(Court court,LocalTime startHour,LocalTime endHour) {

        LocalDate today = LocalDate.now();

        for (int i = 0; i < 30; i++) {
            LocalDate date = today.plusDays(i);
            generateSlots(court, date, startHour, endHour);
        }
    }

}
