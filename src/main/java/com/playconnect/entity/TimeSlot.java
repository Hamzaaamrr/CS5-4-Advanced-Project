package Entity;
import java.time.LocalDate;  // For dates (2026-05-15)
import java.time.LocalTime;  // For times (13:00 PM)

import jakarta.persistence.*;  // JPA annotations (bdl ma akteb SQL)

@Entity  // This class is a database table
@Table(name = "time_slots")  // Table name: time_slots
public class TimeSlot {

    @Id  // PRIMARY KEY
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Database auto-generates ID (1,2,3...)
    private Long id;  // Unique time slot number

    // Many time slots belong to one court 
    @ManyToOne 
 // Foreign key: linking to courts table
    @JoinColumn(name = "court_id")  
 // Which court this slot is for
    private Court court;  

    @Column(nullable = false)  
 // Which day (May 13, 2026)
    private LocalDate date;  

    @Column(nullable = false)  
    // When the slot starts (5:00 PM)
    private LocalTime startTime; 

    @Column(nullable = false)  
 // el endtime: (7:00 PM)
    private LocalTime endTime;  

    @Column(nullable = false) 
    // Is this slot free? true = yes, false = booked
    private boolean available = true;  

    // setters and getters:
    
    public Long getId() {  
        return id;
    }

    public void setId(Long id) {  
        this.id = id;
    }

    public Court getCourt() {  
        return court;
    }

    public void setCourt(Court court) {  
        this.court = court;
    }

    public LocalDate getDate() { 
        return date;
    }

    public void setDate(LocalDate date) {  
        this.date = date;
    }

    public LocalTime getStartTime() {  
        return startTime;
    }

    public void setStartTime(LocalTime startTime) { 
        this.startTime = startTime;
    }

    public LocalTime getEndTime() { 
        return endTime;
    }
    // Set the end time
    public void setEndTime(LocalTime endTime) { 
        // if end time ba3d el start time (7PM after 5PM)
        if(endTime.isAfter(startTime))
        	// Valid time
            this.endTime = endTime;  
        else
        	// Invalid (el end time LAZEM yekon after start time)
            endTime = null;  
    }
 // Is this slot available to book?
    public boolean isAvailable() {  
        return available;
    }
 // Set availability (true = free, false = booked)
    public void setAvailable(boolean available) {  
        this.available = available;
    }
}
