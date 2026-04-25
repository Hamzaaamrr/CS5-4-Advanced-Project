package com.playconnect.service;

import com.playconnect.entity.Court;
import com.playconnect.entity.TimeSlot;
import com.playconnect.repository.TimeSlotRepo;
import org.springframework.stereotype.Service;

import java.time.LocalTime;

@Service
public class TimeSlotService {

    private final TimeSlotRepo timeSlotRepo;

    public TimeSlotService(TimeSlotRepo timeSlotRepo) {
        this.timeSlotRepo = timeSlotRepo;
    }

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

    public boolean isWholeHour(LocalTime time) {
        return time.getMinute() == 0 && time.getSecond() == 0 && time.getNano() == 0;
    }

    public TimeSlot saveSlot(TimeSlot slot) {
        return timeSlotRepo.save(slot);
    }
}