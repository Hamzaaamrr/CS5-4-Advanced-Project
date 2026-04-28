package com.playconnect.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.playconnect.entity.Booking;
import com.playconnect.entity.Court;
import com.playconnect.entity.TimeSlot;
import com.playconnect.entity.User;
import com.playconnect.repository.BookingRepo;
import com.playconnect.repository.CourtRepo;
import com.playconnect.repository.TimeSlotRepo;
import com.playconnect.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service
public class BookingService {

    private final UserRepository userRepo;
    private final CourtRepo courtRepo;
    private final TimeSlotRepo timeSlotRepo;
    private final BookingRepo bookingRepo;

    public BookingService(UserRepository userRepo,CourtRepo courtRepo,TimeSlotRepo timeSlotRepo,BookingRepo bookingRepo) {
        this.userRepo = userRepo;
        this.courtRepo = courtRepo;
        this.timeSlotRepo = timeSlotRepo;
        this.bookingRepo = bookingRepo;
    }

    // =========================
    // MAIN BOOKING FLOW
    // =========================  
    @Transactional
    public Booking createBookingFromRequest(User sessionUser,Long courtId,String date,String startTime,String endTime,int players) {

        // 1. Validate user
        User user = userRepo.findById(sessionUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!user.isActive()) {
            throw new IllegalStateException("User is not active");
        }

        // 2. Validate court
        Court court = courtRepo.findById(courtId)
                .orElseThrow(() -> new IllegalArgumentException("Court not found"));

        // 3. Parse date/time safely
        LocalDate bookingDate = LocalDate.parse(date);
        LocalTime start = LocalTime.parse(startTime);
        LocalTime end = LocalTime.parse(endTime);

        // 4. Validate players
        if (players < 1) {
            throw new IllegalArgumentException("Player count must be at least 1");
        }

        // 5. Validate time logic
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        // 6. Enforce whole-hour rule
        if (!isWholeHour(start) || !isWholeHour(end)) {
            throw new IllegalArgumentException("Booking must be in full hours only");
        }

        // 7. Find exact time slot
        TimeSlot slot = timeSlotRepo
                .findByCourtIdAndDateAndStartTimeAndEndTime(
                        courtId, bookingDate, start, end)
                .orElseThrow(() -> new IllegalArgumentException("Time slot not found"));

        // 8. Check availability
        if (!slot.isAvailable()) {
            throw new IllegalStateException("Time slot already booked");
        }

        // 9. Calculate price
        BigDecimal totalPrice = calculatePrice(court, start, end);

        // 10. Create booking
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setCourt(court);
        booking.setTimeSlot(slot);
        booking.setPlayerCount(players);
        booking.setTotalPrice(totalPrice);
        booking.setBookingStatus(Booking.BookingStatus.CONFIRMED);
        booking.setPaymentStatus(Booking.PaymentStatus.PENDING);

        // 11. Lock slot
        slot.setAvailable(false);

        // 12. Save atomically
        timeSlotRepo.save(slot);
        return bookingRepo.save(booking);
    }

    @Transactional
    public Booking createBookingFromSlotId(User sessionUser, Long slotId, int players) {

        // 1. Validate user
        User user = userRepo.findById(sessionUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!user.isActive()) {
            throw new IllegalStateException("User is not active");
        }

        // 2. Find time slot
        TimeSlot slot = timeSlotRepo.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Time slot not found"));

        // 3. Check availability
        if (!slot.isAvailable()) {
            throw new IllegalStateException("Time slot already booked");
        }

        // 4. Validate players
        if (players < 1) {
            throw new IllegalArgumentException("Player count must be at least 1");
        }

        // 5. Calculate price
        BigDecimal totalPrice = calculatePrice(slot.getCourt(), slot.getStartTime(), slot.getEndTime());

        // 6. Create booking
        Booking booking = new Booking();
        booking.setUser(user);
        booking.setCourt(slot.getCourt());
        booking.setTimeSlot(slot);
        booking.setPlayerCount(players);
        booking.setTotalPrice(totalPrice);
        booking.setBookingStatus(Booking.BookingStatus.CONFIRMED);
        booking.setPaymentStatus(Booking.PaymentStatus.PENDING);

        // 7. Lock slot
        slot.setAvailable(false);

        // 8. Save atomically
        timeSlotRepo.save(slot);
        return bookingRepo.save(booking);
    }

    // =========================
    // CANCEL BOOKING
    // =========================
    @Transactional
    public void cancelBooking(Long bookingId, User sessionUser) {

        Booking booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        User user = userRepo.findById(sessionUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // ownership or admin check
        boolean isOwner = booking.getUser().getId().equals(user.getId());
        boolean isAdmin = "ADMIN".equals(user.getRole());

        if (!isOwner && !isAdmin) {
            throw new IllegalStateException("Not allowed to cancel this booking");
        }

        booking.setBookingStatus(Booking.BookingStatus.CANCELLED);

        TimeSlot slot = booking.getTimeSlot();
        if (slot != null) {
            slot.setAvailable(true);
            timeSlotRepo.save(slot);
        }

        bookingRepo.save(booking);
    }

    // =========================
    // GET USER BOOKINGS
    // =========================
    public List<Booking> getBookingsForUser(Long userId) {
        return bookingRepo.findByUserId(userId);
    }

    // =========================
    // HELPERS
    // =========================
    private boolean isWholeHour(LocalTime time) {
        return time.getMinute() == 0 && time.getSecond() == 0;
    }

    private BigDecimal calculatePrice(Court court, LocalTime start, LocalTime end) {
        long hours = Duration.between(start, end).toHours();
        return court.getPricePerHour().multiply(BigDecimal.valueOf(hours));
    }
}