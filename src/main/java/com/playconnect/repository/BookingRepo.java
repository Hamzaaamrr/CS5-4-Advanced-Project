package repository;
  // This file is in the repository: database operations

import java.util.List;      
import java.util.Optional; 

import org.springframework.data.jpa.repository.JpaRepository;  
import org.springframework.stereotype.Repository;  

import Entity.Booking;  

//This is a Repository (Spring be create the implementation automatically) (bdl ma nekteb SQL)
@Repository  
//<Entity type, ID type>
public interface BookingRepo extends JpaRepository<Booking, Long> {  
    
    // Find ALL bookings bet3et specific user returns a list (momken yekon empty)
	// SELECT * FROM bookings WHERE user_id = ?
    List<Booking> findByUserId(Long userId);  
    
    // Find ALL bookings bet3et specific court returns a list
 // SELECT * FROM bookings WHERE court_id = ?
    List<Booking> findByCourtId(Long courtId);  
    
    // Find one booking by both booking ID and user ID 
    // SELECT * FROM bookings WHERE id = ? AND user_id = ?
    Optional<Booking> findByIdAndUserId(Long id, Long userId); 
}
