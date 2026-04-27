package com.playconnect.service;

import com.playconnect.entity.Court;
import com.playconnect.entity.TimeSlot;
import com.playconnect.repository.TimeSlotRepo;
<<<<<<< HEAD
import org.springframework.stereotype.Service;

import java.time.LocalTime;
=======
import java.util.List;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.Optional;
>>>>>>> main

@Service
public class TimeSlotService {

    private final TimeSlotRepo timeSlotRepo;

    public TimeSlotService(TimeSlotRepo timeSlotRepo) {
        this.timeSlotRepo = timeSlotRepo;
    }

<<<<<<< HEAD
    public TimeSlot getOrCreateSlot(TimeSlot requestedSlot, Court court) {
        return timeSlotRepo.findByCourtIdAndDateAndStartTimeAndEndTime(
                court.getId(), requestedSlot.getDate(), requestedSlot.getStartTime(), requestedSlot.getEndTime())
                .orElseGet(() -> {
                    TimeSlot newSlot = new TimeSlot();
                    newSlot.setCourt(court);
                    newSlot.setDate(requestedSlot.getDate());
                    newSlot.setStartTime(requestedSlot.getStartTime());
                    newSlot.setEndTime(requestedSlot.getEndTime());
                    newSlot.setAvailable(true);
                    return timeSlotRepo.save(newSlot);
                });
    }

    public boolean hasOverlap(TimeSlot slot, Court court) {
        return timeSlotRepo.findByCourtIdAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
                court.getId(), slot.getDate(), slot.getEndTime(), slot.getStartTime())
                .stream()
                .anyMatch(existing -> !existing.isAvailable());
    }

=======
>>>>>>> main
    public boolean isWholeHour(LocalTime time) {
        return time.getMinute() == 0 && time.getSecond() == 0 && time.getNano() == 0;
    }

<<<<<<< HEAD
    public TimeSlot saveSlot(TimeSlot slot) {
        return timeSlotRepo.save(slot);
    }
}
=======
    public TimeSlot createSlot(TimeSlot requestedSlot, Court court) {
        TimeSlot slot = new TimeSlot();
        slot.setCourt(court);
        slot.setDate(requestedSlot.getDate());
        slot.setStartTime(requestedSlot.getStartTime());
        slot.setEndTime(requestedSlot.getEndTime());
        slot.setAvailable(true);
        return timeSlotRepo.save(slot);
    }

    public boolean hasOverlap(TimeSlot slot, Court court) {
        List<TimeSlot> overlappingSlots = timeSlotRepo.findByCourtIdAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
                court.getId(), slot.getDate(), slot.getEndTime(), slot.getStartTime());

        for (TimeSlot existing : overlappingSlots) {
            if (!existing.isAvailable()) {
                return true; // la2ena overlap 3ala booking  
            }
        }
        return false; // No unavailable overlaps found
    }

    public TimeSlot findOrCreateSlot(Court court, TimeSlot requestedSlot) {
        return timeSlotRepo.findByCourtIdAndDateAndStartTimeAndEndTime(
                        court.getId(), requestedSlot.getDate(), requestedSlot.getStartTime(), requestedSlot.getEndTime())
                .orElseGet(() -> createSlot(requestedSlot, court));
    }

    public TimeSlot save(TimeSlot slot) {
        return timeSlotRepo.save(slot);
    }

    public Optional<TimeSlot> findById(Long id) {
        return timeSlotRepo.findById(id);
    }
}
>>>>>>> main
