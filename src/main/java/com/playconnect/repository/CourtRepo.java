package repository;

import Entity.Court;  // Import the court entity

import java.util.List;  // berga3 list court

import org.springframework.data.jpa.repository.JpaRepository;  //(no SQL needed)
import org.springframework.stereotype.Repository; 

//This is a Repository (Spring be create the implementation automatically) (bdl ma nekteb SQL)
@Repository 
// <Entity type, ID type>
public interface CourtRepo extends JpaRepository<Court, Long> { 
    
    // only show courts that are available
	// SELECT * FROM courts WHERE active = true
    List<Court> findByActiveTrue();  
}
