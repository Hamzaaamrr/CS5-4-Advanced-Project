package com.playconnect.service;

import java.math.BigDecimal;
import java.time.Duration;
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

    @Transactional
    public Booking CreateBooking(User u, long id) {
        if (u == null || !u.isActive()) {
            throw new IllegalArgumentException("Invalid user details.");
        }

        Booking booking = BR.findById(id).orElseThrow(() -> new IllegalArgumentException("Booking not found."));

        if (booking.getBookingStatus() == Booking.BookingStatus.CANCELLED) {
            throw new IllegalStateException("Cannot confirm a cancelled booking.");
        }
        if (booking.getBookingStatus() == Booking.BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Booking has already been confirmed.");
        }

        User persistedUser = resolveUser(u);
        Court persistedCourt = resolveCourt(booking.getCourt());
        TimeSlot requestedSlot = booking.getTimeSlot();

        if (requestedSlot == null) {
            throw new IllegalArgumentException("Booking time slot is required.");
        }
        if (booking.getPlayerCount() < 1) {
            throw new IllegalArgumentException("playerCount must be at least 1.");
        }
        LocalTime start = requestedSlot.getStartTime();
        LocalTime end = requestedSlot.getEndTime();
        if (start == null || end == null) {
            throw new IllegalArgumentException("Start and end times are required.");
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("End time must be after start time.");
        }
        if (!isWholeHour(start) || !isWholeHour(end)) {
            throw new IllegalArgumentException("Times must be whole hours.");
        }

        TimeSlot slot = TR.findByCourtIdAndDateAndStartTimeAndEndTime(
                persistedCourt.getId(), requestedSlot.getDate(), start, end)
                .orElseGet(() -> createSlot(requestedSlot, persistedCourt));

        if (!slot.isAvailable()) {
            throw new IllegalStateException("Selected time slot is already booked.");
        }

        // if (hasOverlap(slot, persistedCourt)) {
        //     throw new IllegalStateException("Requested time range overlaps an unavailable slot.");
        // }

        BigDecimal totalPrice = calculateTotalPrice(slot, persistedCourt);

        booking.setUser(persistedUser);
        booking.setCourt(persistedCourt);
        booking.setTimeSlot(slot);
        booking.setTotalPrice(totalPrice);
        booking.setBookingStatus(Booking.BookingStatus.CONFIRMED);
        booking.setPaymentStatus(Booking.PaymentStatus.PENDING);

        slot.setAvailable(false);

        try {
            TR.save(slot);
            return BR.save(booking);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("Booking failed because the timeslot was taken. Please try again.", ex);
        }
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

    private TimeSlot createSlot(TimeSlot requestedSlot, Court court) {
        TimeSlot slot = new TimeSlot();
        slot.setCourt(court);
        slot.setDate(requestedSlot.getDate());
        slot.setStartTime(requestedSlot.getStartTime());
        slot.setEndTime(requestedSlot.getEndTime());
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
