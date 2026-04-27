package com.playconnect.service;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.playconnect.entity.Booking;
import com.playconnect.entity.Court;
import com.playconnect.entity.TimeSlot;
import com.playconnect.entity.User;
import com.playconnect.repository.BookingRepo;


@Service
public class BookingService {
    // Create And Cancel Bookings Functions
    private final UserService userService;
    private final TimeSlotService timeSlotService;
    private final CourtService courtService;
    private final BookingRepo bookingRepo;

    public BookingService(CourtService courtService, TimeSlotService timeSlotService, UserService userService, BookingRepo bookingRepo) {
        this.courtService = courtService;
        this.timeSlotService = timeSlotService;
        this.userService = userService;
        this.bookingRepo = bookingRepo;
    }

    public List<Booking> getBookingsForUser() {
        List<Booking> all = bookingRepo.findAll();
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
        Optional<Booking> bookingOptional = bookingRepo.findById(id);
        if (bookingOptional.isEmpty()) {
            throw new IllegalArgumentException("Booking not found.");
        }
        Booking booking = bookingOptional.get();

        if (booking.getBookingStatus() == Booking.BookingStatus.CANCELLED) {
            return;
        }

        if (user != null && !isBookingOwnerOrAdmin(booking, user)) {
            throw new IllegalStateException("You do not have permission to cancel this booking.");
        }

        booking.setBookingStatus(Booking.BookingStatus.CANCELLED);
        bookingRepo.save(booking);

        TimeSlot timeSlot = booking.getTimeSlot();
        if (timeSlot != null) {
            timeSlot.setAvailable(true);
            timeSlotService.save(timeSlot);
        }
    }

    private boolean isBookingOwnerOrAdmin(Booking booking, User user) {
        return (booking.getUser() != null && booking.getUser().getId() != null
                && booking.getUser().getId().equals(user.getId())) || user.isAdmin();
    }

    @Transactional
    public Booking CreateBooking(User u, long id) {
        if (u == null || !u.valid()) {
            throw new IllegalArgumentException("Invalid user details.");
        }

        Optional<Booking> bookingOptional = bookingRepo.findById(id);
        if (bookingOptional.isEmpty()) {
            throw new IllegalArgumentException("Booking not found.");
        }
        Booking booking = bookingOptional.get();

        if (booking.getBookingStatus() == Booking.BookingStatus.CANCELLED) {
            throw new IllegalStateException("Cannot confirm a cancelled booking.");
        }
        if (booking.getBookingStatus() == Booking.BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Booking has already been confirmed.");
        }

        Optional<User> userOptional = userService.resolveUser(u);
        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("User does not exist.");
        }
        User persistedUser = userOptional.get();

        Optional<Court> courtOptional = courtService.resolveCourt(booking.getCourt());
        if (courtOptional.isEmpty()) {
            throw new IllegalArgumentException("Court does not exist.");
        }
        Court persistedCourt = courtOptional.get();
        
        Optional<TimeSlot> requestedSlotOptional = Optional.ofNullable(booking.getTimeSlot());
        if (requestedSlotOptional.isEmpty()) {
            throw new IllegalArgumentException("Booking time slot is required.");
        }
        TimeSlot requestedSlot = requestedSlotOptional.get();
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
        if (!timeSlotService.isWholeHour(start) || !timeSlotService.isWholeHour(end)) {
            throw new IllegalArgumentException("Times must be whole hours.");
        }

        TimeSlot slot = timeSlotService.findOrCreateSlot(persistedCourt, requestedSlot);

        if (!slot.isAvailable()) {
            throw new IllegalStateException("Selected time slot is already booked.");
        }

        if (timeSlotService.hasOverlap(slot, persistedCourt)) {
            throw new IllegalStateException("Requested time range overlaps an unavailable slot.");
        }

        BigDecimal totalPrice = courtService.calculateTotalPrice(slot, persistedCourt);

        booking.setUser(persistedUser);
        booking.setCourt(persistedCourt);
        booking.setTimeSlot(slot);
        booking.setTotalPrice(totalPrice);
        booking.setBookingStatus(Booking.BookingStatus.CONFIRMED);
        booking.setPaymentStatus(Booking.PaymentStatus.PENDING);

        slot.setAvailable(false);

        try {
            timeSlotService.save(slot);
            return bookingRepo.save(booking);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException("Booking failed because the timeslot was taken. Please try again.", ex);
        }
    }

}
