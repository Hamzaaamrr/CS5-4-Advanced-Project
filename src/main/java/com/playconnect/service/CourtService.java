package com.playconnect.service;

import com.playconnect.entity.Court;
import com.playconnect.entity.TimeSlot;
import com.playconnect.entity.User;
import com.playconnect.repository.CourtRepo;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Service
public class CourtService {
    //Create and Delete Court <--- "Only Admin Can Do These"
private final CourtRepo courtRepo;
public CourtService(CourtRepo courtRepo){
    this.courtRepo = courtRepo;
}
    public Optional<List<Court>> getAllCourts() {
        return Optional.ofNullable(courtRepo.findAll());
    }
    public Court addCourt(Court court ,User user) {
        if(user.isAdmin()){
            return courtRepo.save(court);
        }
        throw new IllegalArgumentException("Only admin can add courts");
    }
    public void deleteCourt(Court court , User user){
        if(user.isAdmin()){
            courtRepo.delete(court);
            return;
        }
        throw new IllegalArgumentException("Only admin can delete courts");
    }

    public Optional<Court> resolveCourt(Court court) {
        if (court == null || court.getId() == null) {
            return Optional.empty();
        }
        return courtRepo.findById(court.getId());
    }

    public BigDecimal calculateTotalPrice(TimeSlot slot, Court court) {
        long hours = Duration.between(slot.getStartTime(), slot.getEndTime()).toHours();
        if (hours <= 0) {
            throw new IllegalArgumentException("Booking duration must be at least one hour.");
        }
        return court.getPricePerHour().multiply(BigDecimal.valueOf(hours));
    }

    public Optional<Court> findCourtById(Long id) {
        return courtRepo.findById(id);  
    }
}
