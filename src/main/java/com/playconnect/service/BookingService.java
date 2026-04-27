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
<<<<<<< HEAD

=======
>>>>>>> main


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
<<<<<<< HEAD
        Optional<Booking> bookingOpt = bookingRepo.findById(id);
        if (bookingOpt.isEmpty()) {
            return;
        }
        Booking booking = bookingOpt.get();
=======
        Optional<Booking> bookingOptional = bookingRepo.findById(id);
        if (bookingOptional.isEmpty()) {
            throw new IllegalArgumentException("Booking not found.");
        }
        Booking booking = bookingOptional.get();
>>>>>>> main

        if (booking.getBookingStatus() == Booking.BookingStatus.CANCELLED) {
            return;
        }

        if (user != null && !isBookingOwnerOrAdmin(booking, user)) {
            return;
        }

        booking.setBookingStatus(Booking.BookingStatus.CANCELLED);
        bookingRepo.save(booking);

        TimeSlot timeSlot = booking.getTimeSlot();
        if (timeSlot != null) {
            timeSlot.setAvailable(true);
<<<<<<< HEAD
            timeSlotService.saveSlot(timeSlot);
=======
            timeSlotService.save(timeSlot);
>>>>>>> main
        }
    }

    private boolean isBookingOwnerOrAdmin(Booking booking, User user) {
        return (booking.getUser() != null && booking.getUser().getId() != null
                && booking.getUser().getId().equals(user.getId())) || user.isAdmin();
    }

    @Transactional
    public Optional<Booking> CreateBooking(User u, long id) {
        if (u == null || !u.valid()) {
            return Optional.empty();
        }

<<<<<<< HEAD
        Optional<Booking> bookingOpt = bookingRepo.findById(id);
        if (bookingOpt.isEmpty()) {
            return Optional.empty();
        }
        Booking booking = bookingOpt.get();
=======
        Optional<Booking> bookingOptional = bookingRepo.findById(id);
        if (bookingOptional.isEmpty()) {
            throw new IllegalArgumentException("Booking not found.");
        }
        Booking booking = bookingOptional.get();
>>>>>>> main

        if (booking.getBookingStatus() == Booking.BookingStatus.CANCELLED) {
            return Optional.empty();
        }
        if (booking.getBookingStatus() == Booking.BookingStatus.CONFIRMED) {
            return Optional.empty();
        }

<<<<<<< HEAD
        Optional<User> persistedUserOpt = userService.resolveUser(u);
        if (persistedUserOpt.isEmpty()) {
            return Optional.empty();
        }
        User persistedUser = persistedUserOpt.get();

        Optional<Court> persistedCourtOpt = courtService.resolveCourt(booking.getCourt());
        if (persistedCourtOpt.isEmpty()) {
            return Optional.empty();
        }
        Court persistedCourt = persistedCourtOpt.get();
        TimeSlot requestedSlot = booking.getTimeSlot();

        if (requestedSlot == null) {
            return Optional.empty();
=======
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
>>>>>>> main
        }
        TimeSlot requestedSlot = requestedSlotOptional.get();
        if (booking.getPlayerCount() < 1) {
            return Optional.empty();
        }
        LocalTime start = requestedSlot.getStartTime();
        LocalTime end = requestedSlot.getEndTime();
        if (start == null || end == null) {
            return Optional.empty();
        }
        if (!end.isAfter(start)) {
            return Optional.empty();
        }
        if (!timeSlotService.isWholeHour(start) || !timeSlotService.isWholeHour(end)) {
<<<<<<< HEAD
            return Optional.empty();
        }

        TimeSlot slot = timeSlotService.getOrCreateSlot(requestedSlot, persistedCourt);
=======
            throw new IllegalArgumentException("Times must be whole hours.");
        }

        TimeSlot slot = timeSlotService.findOrCreateSlot(persistedCourt, requestedSlot);
>>>>>>> main

        if (!slot.isAvailable()) {
            return Optional.empty();
        }

        if (timeSlotService.hasOverlap(slot, persistedCourt)) {
<<<<<<< HEAD
            return Optional.empty();
        }

        Optional<BigDecimal> totalPriceOpt = calculateTotalPrice(slot, persistedCourt);
        if (totalPriceOpt.isEmpty()) return Optional.empty();
        BigDecimal totalPrice = totalPriceOpt.get();
=======
            throw new IllegalStateException("Requested time range overlaps an unavailable slot.");
        }

        BigDecimal totalPrice = courtService.calculateTotalPrice(slot, persistedCourt);
>>>>>>> main

        booking.setUser(persistedUser);
        booking.setCourt(persistedCourt);
        booking.setTimeSlot(slot);
        booking.setTotalPrice(totalPrice);
        booking.setBookingStatus(Booking.BookingStatus.CONFIRMED);
        booking.setPaymentStatus(Booking.PaymentStatus.PENDING);

        slot.setAvailable(false);

        try {
<<<<<<< HEAD
            timeSlotService.saveSlot(slot);
            return Optional.of(bookingRepo.save(booking));
=======
            timeSlotService.save(slot);
            return bookingRepo.save(booking);
>>>>>>> main
        } catch (DataIntegrityViolationException ex) {
            return Optional.empty();
        }
    }

<<<<<<< HEAD
    private Optional<BigDecimal> calculateTotalPrice(TimeSlot slot, Court court) {
        long hours = Duration.between(slot.getStartTime(), slot.getEndTime()).toHours();
        if (hours <= 0) {
            return Optional.empty();
        }
        return Optional.of(court.getPricePerHour().multiply(BigDecimal.valueOf(hours)));
    }
=======
>>>>>>> main
}
