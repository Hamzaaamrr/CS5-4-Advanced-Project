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

@Service  // Service class
public class CourtService {
    
    @Autowired  // Spring injects CourtRepo here
    private CourtRepo courtRepo;  // Repository for DB operations

    @Autowired
    private BookingRepo bookingRepo;

    @Autowired
    private TimeSlotRepo timeSlotRepo;
    
    // Get all active courts (isActive = true)
    public List<Court> getActiveCourts() {
        return courtRepo.findByActiveTrue();  // Query: SELECT * FROM courts WHERE active = true
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
        return courtRepo.findAll();  // Query: SELECT * FROM courts (all courts regardless of active status)
    }

    // Create a new court 
    public Court createCourt(String name, String description, String sportType, String address, BigDecimal pricePerHour) {
        return createCourt(name, description, sportType, address, pricePerHour, null);
    }

    public Court createCourt(String name, String description, String sportType, String address, BigDecimal pricePerHour, MultipartFile thumbnailFile) {
        Court court = new Court();  // Create new Court object
        court.setName(name);  // Set name
        court.setDescription(description);  // Set description
        court.setSportType(sportType);  // Set sport type
        court.setAddress(address);  // Set address
        court.setPricePerHour(pricePerHour);  // Set price
        court.setActive(true);  // Force active = true (court is available)

        if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
            try {
                String contentType = thumbnailFile.getContentType();
                String encoded = Base64.getEncoder().encodeToString(thumbnailFile.getBytes());
                court.setThumbnailData("data:" + (contentType != null ? contentType : "image/jpeg") + ";base64," + encoded);
            } catch (IOException ex) {
                throw new IllegalStateException("Unable to read uploaded court photo.", ex);
            }
        }

        return courtRepo.save(court);  // Save to database and return saved court
    }
    
    // Get court by ID
    public Court getCourtById(Long id) {
        Optional<Court> court = courtRepo.findById(id);  // Find court in DB by ID
        if (court.isPresent()) {  // if	court exists Return the court object
            return court.get();   
        }
        return null;  // else Return null 
    }
    
    // Delete court (soft delete (not from the DB) (just mark as inactive))
    public boolean deleteCourt(Long id) {
        Optional<Court> court = courtRepo.findById(id);  // Find court in DB
        if (court.isPresent()) {  // if court exists
            Court existingCourt = court.get();  // Get the court object
            existingCourt.setActive(false);  // Mark as inactive (soft delete)
            courtRepo.save(existingCourt);  // Save changes to DB
            return true;  // Return true (success)
        }
        return false;  // else Return false (court not found)
    }
    
    // Hard delete (completely remove from database) - optional
    @Transactional
    public boolean hardDeleteCourt(Long id) {
        if (!courtRepo.existsById(id)) {  // Check if court exists
            return false;  // else Return false (court not found)
        }

        List<Booking> bookings = bookingRepo.findByCourtId(id);
        bookingRepo.deleteAll(bookings);

        List<TimeSlot> timeSlots = timeSlotRepo.findByCourtId(id);
        timeSlotRepo.deleteAll(timeSlots);

        courtRepo.deleteById(id);  // Permanently delete from DB
        return true;  // Return true (success)
    }
    // Update court information
    public Court updateCourt(Long id, String name, String description, String sportType, String address, BigDecimal pricePerHour) {
        Optional<Court> court = courtRepo.findById(id);  // Find court in DB
        if (court.isPresent()) {  // If court exists
            Court existingCourt = court.get();  // Get the court object
            existingCourt.setName(name);  // Update name
            existingCourt.setDescription(description);  // Update description
            existingCourt.setSportType(sportType);  // Update sport type
            existingCourt.setAddress(address);  // Update address
            existingCourt.setPricePerHour(pricePerHour);  // Update price
            return courtRepo.save(existingCourt);  // Save and return updated court
        }
        return null;  // else Return null (court not found)
    }

}