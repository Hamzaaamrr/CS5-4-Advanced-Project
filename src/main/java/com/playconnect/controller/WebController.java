package com.playconnect.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.playconnect.entity.Court;
import com.playconnect.entity.TimeSlot;
import com.playconnect.entity.User;
import com.playconnect.service.BookingService;
import com.playconnect.service.CourtService;
import com.playconnect.service.TimeSlotService;
import com.playconnect.service.UserService;

import jakarta.servlet.http.HttpSession;

@Controller
public class WebController {

    private final UserService userService;
    private final CourtService courtService;
    private final TimeSlotService timeSlotService;
    private final BookingService bookingService;

    public WebController(UserService userService,CourtService courtService,TimeSlotService timeSlotService,BookingService bookingService) {
        this.userService = userService;
        this.courtService = courtService;
        this.timeSlotService = timeSlotService;
        this.bookingService = bookingService;
    }

    // =========================
    // AUTH
    // =========================

    @GetMapping("/login")
    public String renderLoginPage() {
        return "login";
    }

    @PostMapping("/login")
    public String handleLogin(@RequestParam String email,@RequestParam String password,Model model,HttpSession session) {

        User user = userService.authenticate(email, password);

        if (user != null) {
            session.setAttribute("loggedUser", user);
            return "redirect:/home";
        }

        model.addAttribute("error", "Invalid credentials");
        return "login";
    }

    @GetMapping("/register")
    public String renderRegisterPage() {
        return "register";
    }

    @PostMapping("/register")
    public String handleRegister(@RequestParam String firstName,@RequestParam String lastName,@RequestParam String username,@RequestParam String email,@RequestParam String password,Model model) {

        boolean success = userService.registerPlayer(firstName, lastName, username, email, password);

        if (!success) {
            model.addAttribute("error", "Username or email already exists");
            return "register";
        }

        return "redirect:/login";
    }

    // =========================
    // HOME
    // =========================

    @GetMapping("/home")
    public String renderHomePage(HttpSession session, Model model) {

        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("user", user.getFullName());
        model.addAttribute("courts", courtService.getActiveCourts());
        model.addAttribute("bookings",bookingService.getBookingsForUser(user.getId()));

        return "home";
    }

    // =========================
    // BOOKING
    // =========================

    @GetMapping("/booking")
    public String renderBookingPage(Model model) {
        model.addAttribute("courts", courtService.getActiveCourts());
        return "booking";
    }

    @GetMapping("/select-slots")
    public String renderSelectSlotsPage(@RequestParam Long courtId, @RequestParam String date, Model model) {
        Court court = courtService.getCourtById(courtId);
        List<TimeSlot> slots = timeSlotService.getAvailableSlotsByDate(courtId, LocalDate.parse(date));
        model.addAttribute("court", court);
        model.addAttribute("date", date);
        model.addAttribute("slots", slots);
        return "select-slots";
    }

    @PostMapping("/book")
    public String handleBooking(@RequestParam Long slotId,@RequestParam int players,HttpSession session,Model model) {

        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        try {
            bookingService.createBookingFromSlotId(user, slotId, players);
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "error"; // simple error page
        }

        return "redirect:/my-bookings";
    }

    // =========================
    // MY BOOKINGS
    // =========================

    @GetMapping("/my-bookings")
    public String renderMyBookings(HttpSession session, Model model) {

        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("bookings",bookingService.getBookingsForUser(user.getId()));

        return "my-bookings";
    }

    @PostMapping("/cancel")
    public String handleCancelBooking(@RequestParam Long bookingId,HttpSession session) {

        User user = (User) session.getAttribute("loggedUser");

        if (user == null) {
            return "redirect:/login";
        }

        bookingService.cancelBooking(bookingId, user);

        return "redirect:/my-bookings";
    }

    // =========================
    // SLOTS (API)
    // =========================

    @GetMapping("/slots")
    @ResponseBody
    public List<TimeSlot> fetchAvailableSlots(@RequestParam Long courtId,@RequestParam String date) {

        return timeSlotService.getAvailableSlotsByDate(courtId,LocalDate.parse(date));
    }

    // =========================
    // LOGOUT
    // =========================

    @GetMapping("/logout")
    public String handleLogout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }




    private User getCurrentUser(HttpSession session) {
        return (User) session.getAttribute("loggedUser");
    }

    private boolean isLoggedIn(HttpSession session) {
        return getCurrentUser(session) != null;
    }

    private boolean isAdmin(HttpSession session) {
        User user = getCurrentUser(session);
        return user != null && "ADMIN".equals(user.getRole());
    }


    @GetMapping("/admin/courts/new")
    public String renderCreateCourt(HttpSession session) {

        if (!isAdmin(session)) {
            return "redirect:/home";
        }

        return "create-court";
    }

    @PostMapping("/admin/courts")
    public String handleCreateCourt(@RequestParam String name,@RequestParam String description,@RequestParam String sportType,@RequestParam String address,@RequestParam BigDecimal price,HttpSession session) {

        // 🔐 Admin protection
        if (!isAdmin(session)) {
            return "redirect:/home";
        }

        // ✅ 1. Create court
        Court court = courtService.createCourt(name, description, sportType, address, price);

        // ✅ 2. Generate slots for this court
        timeSlotService.generateSlotsForNextDays(court,LocalTime.of(10, 0),LocalTime.of(22, 0));

        // ✅ 3. Redirect
        return "redirect:/home";
    }



    @GetMapping("/courts")
    public String renderCourtsPage(HttpSession session, Model model) {

        if (!isLoggedIn(session)) {
            return "redirect:/login";
        }

        model.addAttribute("courts", courtService.getActiveCourts());

        return "courts";
    }


}