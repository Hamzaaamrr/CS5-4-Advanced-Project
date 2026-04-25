package com.playconnect.service;

import com.playconnect.entity.Court;
import com.playconnect.repository.CourtRepo;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CourtService {
    private final CourtRepo courtRepo;

    public CourtService(CourtRepo courtRepo) {
        this.courtRepo = courtRepo;
    }

    public Optional<Court> resolveCourt(Court court) {
        if (court == null || court.getId() == null) {
            return Optional.empty();
        }
        return courtRepo.findById(court.getId());
    }
}
