package com.playconnect.service;

import com.playconnect.entity.Court;
import com.playconnect.entity.Booking;
import com.playconnect.entity.TimeSlot;
import com.playconnect.repository.CourtRepo;
import com.playconnect.repository.BookingRepo;
import com.playconnect.repository.TimeSlotRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Service
public class CourtService {
    
    @Autowired
    private CourtRepo courtRepo;

    @Autowired
    private BookingRepo bookingRepo;

    @Autowired
    private TimeSlotRepo timeSlotRepo;
    
    public List<Court> getActiveCourts() {
        return courtRepo.findByActiveTrue();
    }


    public List<Court> searchActiveCourts(String searchTerm) {
        List<Court> courts = getActiveCourts();
        if (searchTerm == null || searchTerm.isBlank()) {
            return courts;
        }

        String normalizedSearch = searchTerm.trim().toLowerCase();
        return courts.stream()
                .filter(court -> (court.getName() != null && court.getName().toLowerCase().contains(normalizedSearch))
                || (court.getAddress() != null && court.getAddress().toLowerCase().contains(normalizedSearch))
                || (court.getSportType() != null && court.getSportType().toLowerCase().contains(normalizedSearch)))
                .toList();
    }
    
    public List<Court> getAllCourts() {
        return courtRepo.findAll();
    }

    public Court createCourt(String name, String description, String sportType, String address, BigDecimal pricePerHour) {
        return createCourt(name, description, sportType, address, pricePerHour, null);
    }

    public Court createCourt(String name, String description, String sportType, String address, BigDecimal pricePerHour, MultipartFile thumbnailFile) {
        Court court = new Court();
        court.setName(name);
        court.setDescription(description);
        court.setSportType(sportType);
        court.setAddress(address);
        court.setPricePerHour(pricePerHour);
        court.setActive(true);

        if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
            try {
                String contentType = thumbnailFile.getContentType();
                String encoded = Base64.getEncoder().encodeToString(thumbnailFile.getBytes());
                court.setThumbnailData("data:" + (contentType != null ? contentType : "image/jpeg") + ";base64," + encoded);
            } catch (IOException ex) {
                throw new IllegalStateException("Unable to read uploaded court photo.", ex);
            }
        }

        return courtRepo.save(court);
    }
    
    public Court getCourtById(Long id) {
        Optional<Court> court = courtRepo.findById(id);
        if (court.isPresent()) {
            return court.get();
        }
        return null;
    }
    
    
    
    @Transactional
    public boolean hardDeleteCourt(Long id) {
        if (!courtRepo.existsById(id)) {
            return false;
        }

        List<Booking> bookings = bookingRepo.findByCourtId(id);
        bookingRepo.deleteAll(bookings);

        List<TimeSlot> timeSlots = timeSlotRepo.findByCourtId(id);
        timeSlotRepo.deleteAll(timeSlots);

        courtRepo.deleteById(id);
        return true;
    }
    public Court updateCourt(Long id, String name, String description, String sportType, String address, BigDecimal pricePerHour) {
        Optional<Court> court = courtRepo.findById(id);
        if (court.isPresent()) {
            Court existingCourt = court.get();
            existingCourt.setName(name);
            existingCourt.setDescription(description);
            existingCourt.setSportType(sportType);
            existingCourt.setAddress(address);
            existingCourt.setPricePerHour(pricePerHour);
            return courtRepo.save(existingCourt);
        }
        return null;
    }

}