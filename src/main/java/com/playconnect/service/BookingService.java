package com.playconnect.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.playconnect.entity.Booking;
import com.playconnect.entity.Court;
import com.playconnect.entity.TimeSlot;
import com.playconnect.entity.User;
import com.playconnect.repository.BookingRepo;
import com.playconnect.repository.CourtRepo;
import com.playconnect.repository.TimeSlotRepo;
import com.playconnect.repository.UserRepository;


@Service
public class BookingService {
    // Create And Cancel Bookings Functions
    private final UserRepository UR;
    private final TimeSlotRepo TR;
    private final CourtRepo CR;
    private final BookingRepo BR;

    public BookingService(CourtRepo CR, TimeSlotRepo TR, UserRepository UR, BookingRepo BR) {
        this.CR = CR;
        this.TR = TR;
        this.UR = UR;
        this.BR = BR;
    }

    public List<Booking> getBookingsForUser() {
        List<Booking> all = BR.findAll();
        List<Booking> bookings = new ArrayList<>();

        for (Booking b : all) {
            if (b.getBookingStatus().equals(Booking.BookingStatus.PENDING)) {
                bookings.add(b);
            }
        }

        return bookings;
    }

    public List<Booking> getBookingsForUser(Long userId) {
        User user = UR.findById(userId).orElse(null);
        if (user == null) {
            return new ArrayList<>();
        }

        List<Booking> all = BR.findAll();
        List<Booking> userBookings = new ArrayList<>();

        for (Booking b : all) {
            if (b.getUser() != null && b.getUser().getId() != null && 
                b.getUser().getId().equals(userId) && 
                (b.getBookingStatus().equals(Booking.BookingStatus.CONFIRMED) || 
                 b.getBookingStatus().equals(Booking.BookingStatus.PENDING))) {
                userBookings.add(b);
            }
        }

        return userBookings;
    }

    @Transactional
    public void CancelBooking(long id, User user) {
        Booking booking = BR.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found."));

        if (booking.getBookingStatus() == Booking.BookingStatus.CANCELLED) {
            return;
        }

        if (user != null && !isBookingOwnerOrAdmin(booking, user)) {
            throw new IllegalStateException("You do not have permission to cancel this booking.");
        }

        booking.setBookingStatus(Booking.BookingStatus.CANCELLED);
        BR.save(booking);

        TimeSlot timeSlot = booking.getTimeSlot();
        if (timeSlot != null) {
            timeSlot.setAvailable(true);
            TR.save(timeSlot);
        }
    }

    private boolean isBookingOwnerOrAdmin(Booking booking, User user) {
        return (booking.getUser() != null && booking.getUser().getId() != null
                && booking.getUser().getId().equals(user.getId())) || user.isAdmin();
    }

    private User resolveUser(User user) {
        if (user.getId() != null) {
            return UR.findById(user.getId())
                    .orElseThrow(() -> new IllegalArgumentException("User does not exist."));
        }
        return UR.findByEmail(user.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User does not exist."));
    }

    private Court resolveCourt(Court court) {
        if (court == null || court.getId() == null) {
            throw new IllegalArgumentException("Booking court is required.");
        }
        return CR.findById(court.getId())
                .orElseThrow(() -> new IllegalArgumentException("Court does not exist."));
    }

    private boolean isWholeHour(LocalTime time) {
        return time.getMinute() == 0 && time.getSecond() == 0 && time.getNano() == 0;
    }

    //REMADE CREATE BOOKING FUNCTION
    @Transactional
    public Booking createBooking(User user, Court court, LocalDate date, LocalTime startTime, LocalTime endTime) {
        if (user == null || !user.isActive()) {
            throw new IllegalArgumentException("Invalid user details.");
        }

        User persistedUser = resolveUser(user);
        Court persistedCourt = resolveCourt(court);

        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("Start and end times are required.");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("End time must be after start time.");
        }
        if (!isWholeHour(startTime) || !isWholeHour(endTime)) {
            throw new IllegalArgumentException("Times must be whole hours.");
        }

        TimeSlot slot = TR.findByCourtIdAndDateAndStartTimeAndEndTime(
                persistedCourt.getId(), date, startTime, endTime)
                .orElseGet(() -> createSlot(new TimeSlot(), persistedCourt, date, startTime, endTime));

        if (!slot.isAvailable()) {
            throw new IllegalStateException("Selected time slot is already booked.");
        }

        BigDecimal totalPrice = calculateTotalPrice(slot, persistedCourt);

        Booking booking = new Booking();
        booking.setUser(persistedUser);
        booking.setCourt(persistedCourt);
        booking.setTimeSlot(slot);
        booking.setTotalPrice(totalPrice);
        booking.setPlayerCount(1);
        booking.setBookingStatus(Booking.BookingStatus.CONFIRMED);

        slot.setAvailable(false);

        try {
            TR.save(slot);
            return BR.save(booking);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("Booking failed because the timeslot was taken. Please try again.", ex);
        }
    }

    private TimeSlot createSlot(TimeSlot requestedSlot, Court court, LocalDate date, LocalTime startTime, LocalTime endTime) {
        TimeSlot slot = new TimeSlot();
        slot.setCourt(court);
        slot.setDate(date);
        slot.setStartTime(startTime);
        slot.setEndTime(endTime);
        slot.setAvailable(true);
        return TR.save(slot);
    }


    // private boolean hasOverlap(TimeSlot slot, Court court) {
    //     return TR.findByCourtIdAndDateAndStartTimeLessThanAndEndTimeGreaterThan(
    //             court.getId(), slot.getDate(), slot.getEndTime(), slot.getStartTime())
    //             .stream()
    //             .anyMatch(existing -> !existing.isAvailable());
    // }

    private BigDecimal calculateTotalPrice(TimeSlot slot, Court court) {
        long hours = Duration.between(slot.getStartTime(), slot.getEndTime()).toHours();
        if (hours <= 0) {
            throw new IllegalArgumentException("Booking duration must be at least one hour.");
        }
        return court.getPricePerHour().multiply(BigDecimal.valueOf(hours));
    }

    
}
