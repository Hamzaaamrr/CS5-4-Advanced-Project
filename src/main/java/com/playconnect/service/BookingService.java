package com.playconnect.service;

import java.math.BigDecimal;
import java.time.Duration;
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
        Optional<Booking> bookingOpt = bookingRepo.findById(id);
        if (bookingOpt.isEmpty()) {
            return;
        }
        Booking booking = bookingOpt.get();

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
            timeSlotService.saveSlot(timeSlot);
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

        Optional<Booking> bookingOpt = bookingRepo.findById(id);
        if (bookingOpt.isEmpty()) {
            return Optional.empty();
        }
        Booking booking = bookingOpt.get();

        if (booking.getBookingStatus() == Booking.BookingStatus.CANCELLED) {
            return Optional.empty();
        }
        if (booking.getBookingStatus() == Booking.BookingStatus.CONFIRMED) {
            return Optional.empty();
        }

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
        }
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
            return Optional.empty();
        }

        TimeSlot slot = timeSlotService.getOrCreateSlot(requestedSlot, persistedCourt);

        if (!slot.isAvailable()) {
            return Optional.empty();
        }

        if (timeSlotService.hasOverlap(slot, persistedCourt)) {
            return Optional.empty();
        }

        Optional<BigDecimal> totalPriceOpt = calculateTotalPrice(slot, persistedCourt);
        if (totalPriceOpt.isEmpty()) return Optional.empty();
        BigDecimal totalPrice = totalPriceOpt.get();

        booking.setUser(persistedUser);
        booking.setCourt(persistedCourt);
        booking.setTimeSlot(slot);
        booking.setTotalPrice(totalPrice);
        booking.setBookingStatus(Booking.BookingStatus.CONFIRMED);
        booking.setPaymentStatus(Booking.PaymentStatus.PENDING);

        slot.setAvailable(false);

        try {
            timeSlotService.saveSlot(slot);
            return Optional.of(bookingRepo.save(booking));
        } catch (DataIntegrityViolationException ex) {
            return Optional.empty();
        }
    }

    private Optional<BigDecimal> calculateTotalPrice(TimeSlot slot, Court court) {
        long hours = Duration.between(slot.getStartTime(), slot.getEndTime()).toHours();
        if (hours <= 0) {
            return Optional.empty();
        }
        return Optional.of(court.getPricePerHour().multiply(BigDecimal.valueOf(hours)));
    }
}
