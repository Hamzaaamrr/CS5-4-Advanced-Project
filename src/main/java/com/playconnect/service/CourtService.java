package com.playconnect.service;

import com.playconnect.entity.Court; 
import com.playconnect.repository.CourtRepo;
import org.springframework.beans.factory.annotation.Autowired;  
import org.springframework.stereotype.Service; 
import java.math.BigDecimal;  
import java.util.List; 
import java.util.Optional;  

@Service  // Service class
public class CourtService {
    
    @Autowired  // Spring injects CourtRepo here
    private CourtRepo courtRepo;  // Repository for DB operations
    
    // Get all active courts (isActive = true)
    public List<Court> getActiveCourts() {
        return courtRepo.findByActiveTrue();  // Query: SELECT * FROM courts WHERE active = true
    }
    
    // Create a new court 
    public Court createCourt(String name, String description, String sportType, String address, BigDecimal pricePerHour) {
        Court court = new Court();  // Create new Court object
        court.setName(name);  // Set name
        court.setDescription(description);  // Set description
        court.setSportType(sportType);  // Set sport type
        court.setAddress(address);  // Set address
        court.setPricePerHour(pricePerHour);  // Set price
        court.setActive(true);  // Force active = true (court is available)
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
    public boolean hardDeleteCourt(Long id) {
        if (courtRepo.existsById(id)) {  // Check if court exists
            courtRepo.deleteById(id);  // Permanently delete from DB
            return true;  // Return true (success)
        }
        return false;  //else Return false (court not found)
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