package Service;

import java.math.BigDecimal;  
import java.time.Duration;  // For calculating hours between times
import java.time.LocalDate;  
import java.time.LocalTime;  
import java.util.ArrayList;  
import java.util.List;  
import java.util.Optional;  // For handling null safely

import org.springframework.dao.DataIntegrityViolationException;  
import org.springframework.stereotype.Service;  
import org.springframework.transaction.annotation.Transactional;  

import Entity.*; 
import repository.*;  

@Service  // This class handles booking business logic (creating, cancelling, viewing)
public class BookingService {
    
    // DEPENDENCIES (Repositories) 
    private final UserRepository UR;  // To access user data
    private final TimeSlotRepo TR;  // To access time slot data
    private final CourtRepo CR;  // To access court data
    private final BookingRepo BR;  // To access booking data
    private final MailService mailService;  // To send emails

    // Constructor: Spring gives us all dependencies
    public BookingService(CourtRepo CR, TimeSlotRepo TR, UserRepository UR, BookingRepo BR, MailService mailService) {
        this.CR = CR;
        this.TR = TR;
        this.UR = UR;
        this.BR = BR;
        this.mailService = mailService;
    }

    // GET ALL BOOKINGS (ADMIN ONLY) 
    public List<Booking> getAllBookings() {
        return BR.findAll();  // (admin only)
    }

    // GET ACTIVE BOOKINGS FOR A USER 
    public List<Booking> getBookingsForUser(Long userId) {
        User user = UR.findById(userId).orElse(null);  // Find user by ID
        if (user == null) {
            return new ArrayList<>();  // Return empty list if user not found
        }

        List<Booking> all = BR.findAll();  // Get all bookings
        List<Booking> userBookings = new ArrayList<>();  // Create empty list for this user's bookings

        // Loop through all bookings and filter for this user
        for (Booking b : all) {
            if (b.getUser() != null && b.getUser().getId() != null && 
                b.getUser().getId().equals(userId) && 
                (b.getBookingStatus().equals(Booking.BookingStatus.CONFIRMED) || 
                 b.getBookingStatus().equals(Booking.BookingStatus.PENDING))) {
                userBookings.add(b);  // Add confirmed or pending bookings
            }
        }
        return userBookings;  // Return only active bookings (not cancelled)
    }

    // GET CANCELLED BOOKINGS FOR A USER 
    public List<Booking> getCancelledBookingsForUser(Long userId) {
        User user = UR.findById(userId).orElse(null);
        if (user == null) {
            return new ArrayList<>();
        }

        List<Booking> all = BR.findAll();
        List<Booking> cancelledBookings = new ArrayList<>();

        // Loop through all bookings and filter cancelled ones
        for (Booking booking : all) {
            if (booking.getUser() != null && booking.getUser().getId() != null &&
                booking.getUser().getId().equals(userId) &&
                booking.getBookingStatus().equals(Booking.BookingStatus.CANCELLED)) {
                cancelledBookings.add(booking);  // Add only cancelled bookings
            }
        }
        return cancelledBookings;
    }

    // CANCEL A BOOKING 
    @Transactional  // If anything fails, all changes are undone
    public void cancelBooking(long id, User user) {
        // Find the booking
        Booking booking = BR.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));

        // If already cancelled, do nothing
        if (booking.getBookingStatus() == Booking.BookingStatus.CANCELLED) {
            return;
        }

        // Check if user has permission (owner OR admin)
        if (user != null && !isBookingOwnerOrAdmin(booking, user)) {
            throw new IllegalStateException("You do not have permission to cancel this booking.");
        }
        
        // Mark as cancelled
        booking.setBookingStatus(Booking.BookingStatus.CANCELLED);
        Booking savedBooking = BR.save(booking);  // Save to database
        
        // Send email notification
        mailService.sendCancellationEmail(user, savedBooking);

        // Free up the time slot (make it available again)
        TimeSlot timeSlot = booking.getTimeSlot();
        if (timeSlot != null) {
            timeSlot.setAvailable(true);
            TR.save(timeSlot);
        }
    }

    // CHECK IF USER CAN CANCEL THIS BOOKING 
    private boolean isBookingOwnerOrAdmin(Booking booking, User user) {
        // Returns true if user owns the booking OR user is admin
        return (booking.getUser() != null && booking.getUser().getId() != null
                && booking.getUser().getId().equals(user.getId())) || user.isAdmin();
    }

    // GET USER FROM DATABASE 
    private User resolveUser(User user) {
        if (user.getId() != null) {
            return UR.findById(user.getId())
                    .orElseThrow(() -> new IllegalArgumentException("User does not exist."));
        }
        return UR.findByEmail(user.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User does not exist."));
    }

    // GET COURT FROM DATABASE 
    private Court resolveCourt(Court court) {
        if (court == null || court.getId() == null) {
            throw new IllegalArgumentException("Booking court is required.");
        }
        return CR.findById(court.getId())
                .orElseThrow(() -> new IllegalArgumentException("Court does not exist."));
    }

    // CHECK IF TIME IS WHOLE HOUR (9:00, 10:00, NOT 9:30) 
    private boolean isWholeHour(LocalTime time) {
        return time.getMinute() == 0 && time.getSecond() == 0 && time.getNano() == 0;
    }

    //  CREATE A NEW BOOKING 
    @Transactional  // If anything fails, all changes are undone
    public Booking createBooking(User user, Court court, LocalDate date, LocalTime startTime, LocalTime endTime) {

        // Validate user
        if (user == null || !user.isActive()) {
            throw new IllegalArgumentException("Invalid user details.");
        }

        // Get actual user and court from database
        User persistedUser = resolveUser(user);
        Court persistedCourt = resolveCourt(court);

        // Validate times
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Start and end times are required.");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("End time must be after start time.");
        }
        if (!isWholeHour(startTime) || !isWholeHour(endTime)) {
            throw new IllegalArgumentException("Times must be whole hours.");
        }

        // Find existing time slot or create new one
        TimeSlot slot = TR.findByCourtIdAndDateAndStartTimeAndEndTime(
                persistedCourt.getId(), date, startTime, endTime)
                .orElseGet(() -> createSlot(new TimeSlot(), persistedCourt, date, startTime, endTime));

        // Check if slot is available
        if (!slot.isAvailable()) {
            throw new IllegalStateException("Selected time slot is already booked.");
        }

        // Calculate total price (price per hour × number of hours)
        BigDecimal totalPrice = calculateTotalPrice(slot, persistedCourt);

        // Create new booking
        Booking booking = new Booking();
        booking.setUser(persistedUser);
        booking.setCourt(persistedCourt);
        booking.setTimeSlot(slot);
        booking.setTotalPrice(totalPrice);
        booking.setPlayerCount(1);
        booking.setBookingStatus(Booking.BookingStatus.CONFIRMED);

        // Mark slot as booked
        slot.setAvailable(false);

        try {
            TR.save(slot);  // Save time slot
            Booking savedBooking = BR.save(booking);  // Save booking
            mailService.sendBookingEmail(persistedUser, savedBooking);  // Send confirmation email
            return savedBooking;
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException(
                    "Booking failed because the timeslot was taken. Please try again.",
                    ex
            );
        }
    }

    // CREATE A NEW TIME SLOT 
    private TimeSlot createSlot(TimeSlot requestedSlot, Court court, LocalDate date, 
                                 LocalTime startTime, LocalTime endTime) {
        TimeSlot slot = new TimeSlot();
        slot.setCourt(court);
        slot.setDate(date);
        slot.setStartTime(startTime);
        slot.setEndTime(endTime);
        slot.setAvailable(true);  // New slot starts as available
        return TR.save(slot);
    }

    //  CALCULATE TOTAL PRICE
    private BigDecimal calculateTotalPrice(TimeSlot slot, Court court) {
        // Calculate number of hours between start and end time
        long hours = Duration.between(slot.getStartTime(), slot.getEndTime()).toHours();
        if (hours <= 0) {
            throw new IllegalArgumentException("Booking duration must be at least one hour.");
        }
        // Price = hours × price per hour
        return court.getPricePerHour().multiply(BigDecimal.valueOf(hours));
    }
}
