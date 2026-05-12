package Entity;

import java.math.BigDecimal;  // For prices (like 500.00 EGP)
import jakarta.persistence.*;  // JPA annotations for database mapping (bdl ma akteb sql )

@Entity  // This class represents a table in the database
@Table(name = "bookings")  // The table name will be "bookings"
public class Booking {

    @Id  // PRIMARY KEY (unique ID for each booking)
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Database auto-generates the ID (1,2,3...)
    private Long id;  // Unique booking number (e.g., 1001)

    @ManyToOne  // a user can have many bookings
    @JoinColumn(name = "user_id")  // Foreign key: column that links to users table
    private User user;  // Who made this booking (e.g., Ahmed)

    @ManyToOne  // a court can have many bookings
    @JoinColumn(name = "court_id")  // Foreign key: column that links to courts table
    private Court court;  // Which court was booked (e.g., Cairo Stadium)

    @ManyToOne  // Many bookings can use ONE time slot, but one slot can have only one booking
    @JoinColumn(name = "time_slot_id")  // Foreign key: column that links to time_slots table
    private TimeSlot timeSlot;  // When the booking is for: date, start time, end time

    @Column(nullable = false)  // This column cannot be empty 
    private BigDecimal totalPrice;  // How much the booking costs (price per hour × hours)

    @Column(nullable = false)  // This column cannot be empty
    private int playerCount = 1;  // Number of players playing (default is 1)

    @Enumerated(EnumType.STRING)  // Store the status as text in database ("PENDING", "CONFIRMED", etc.)
    @Column(nullable = false)  // This column cannot be empty
    private BookingStatus bookingStatus = BookingStatus.PENDING;  // Current status of booking

    // getters and Setters: 
    
    public Long getId() {  
        return id;
    }

    public void setId(Long id) {  
        this.id = id;
    }

    public User getUser() { 
        return user;
    }

    public void setUser(User user) {  
        this.user = user;
    }

    public Court getCourt() { 
        return court;
    }

    public void setCourt(Court court) {  
        this.court = court;
    }

    public TimeSlot getTimeSlot() { 
        return timeSlot;
    }

    public void setTimeSlot(TimeSlot timeSlot) {  
        this.timeSlot = timeSlot;
    }

    public BigDecimal getTotalPrice() {  
        return totalPrice;
    }

    public void setTotalPrice(BigDecimal totalPrice) { 
        this.totalPrice = totalPrice;
    }

    public int getPlayerCount() {  
        return playerCount;
    }

    public void setPlayerCount(int playerCount) {  
        this.playerCount = playerCount;
    }

    public BookingStatus getBookingStatus() {  
        return bookingStatus;
    }

    public void setBookingStatus(BookingStatus bookingStatus) {  
        this.bookingStatus = bookingStatus;
    }

    // ENUM (List of possible status values) 
    // An enum is like a dropdown menu: only these 3 options are allowed
    public enum BookingStatus {
        PENDING,    // Booking is waiting (not yet confirmed)
        CONFIRMED,  // Booking is approved and locked in
        CANCELLED   // Booking was cancelled (time slot is free again)
    }
}
