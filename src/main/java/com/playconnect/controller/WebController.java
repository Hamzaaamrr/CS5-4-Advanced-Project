package com.playconnect.controller;

import com.playconnect.entity.*;
import com.playconnect.enums.SportType;
import com.playconnect.service.*;

import jakarta.servlet.http.HttpSession;

import org.springframework.ui.Model;

// import java.security.Principal;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalTime;

// import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
// import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class WebController {

    private static final String SESSION_USER_ID = "SESSION_USER_ID";
    // private static final String SESSION_CART = "SESSION_CART";

    UserService userService;
    CourtService courtService;
    TimeSlotService timeSlotService;
    BookingService bookingService;

    public WebController(UserService userService, CourtService courtService, TimeSlotService timeSlotService, BookingService bookingService) {
        this.userService = userService;
        this.courtService = courtService;
        this.timeSlotService = timeSlotService;
        this.bookingService = bookingService;
    }

    @GetMapping("/")
    public String rootRedirect(HttpSession session) {
        if (session.getAttribute(SESSION_USER_ID) != null) {
            return "redirect:/home";
        }
        return "redirect:/login";
        
    }

    @GetMapping("/login")
    public String showLoginPage(HttpSession session) {
        if(session.getAttribute(SESSION_USER_ID) != null){
            return "redirect:/home";
        }
        return "login"; // Return to login view
    }

    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password, HttpSession session, Model model) {
        User user = userService.authenticate(email, password);

        if (user != null) {
            session.setAttribute(SESSION_USER_ID, user.getId());
            return "redirect:/home";
        }

        model.addAttribute("error", "Invalid email or password.");
        return "login";
    }

    @PostMapping("/register")
    public String register(@RequestParam String firstName, @RequestParam String lastName, @RequestParam String username, @RequestParam String email, @RequestParam String password, Model model) {
        boolean success = userService.registerPlayer(firstName, lastName, username, email, password);

        if (!success) {
            model.addAttribute("error", "Username or email already exists");
            return "register";
        }

        return "redirect:/login";
    }

    @GetMapping("/register")
    public String showRegisterPage() {
        return "register"; // Return the name of the registration view 
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    @GetMapping("/home")
    public String showCourts(@RequestParam(required = false, defaultValue = "") String search, Model model, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        User currentUser = null;

        if (userId == null) {
            return "redirect:/login";
        }

        currentUser = userService.getUserById(userId);

        model.addAttribute("currentUser", currentUser);

            boolean isAdmin = false;
            if (currentUser != null) {
                model.addAttribute("user", currentUser.getFirstName());
                isAdmin = currentUser.isAdmin();
            }
            model.addAttribute("isAdmin", isAdmin);

            List<Court> courts = courtService.searchActiveCourts(search);
            if (courts == null) {
                courts = List.of(); //0 element list if no courts found
            }

            model.addAttribute("courts", courts);
            model.addAttribute("searchTerm", search);
            return "home";
    }

    @GetMapping("/courts/new")
    public String showCreateCourtPage(Model model, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return "redirect:/login";
        }
        User user = userService.getUserById(userId);
        if (!user.isAdmin()) {
            return "redirect:/home";
        }

        model.addAttribute("currentUser", user);
        model.addAttribute("isAdmin", true);
        model.addAttribute("court", new Court());
        model.addAttribute("sportTypes", SportType.values());
        return "court-form";
    }

    @PostMapping("/courts")
    public String createCourt(@ModelAttribute Court court, @RequestParam(required = false) MultipartFile thumbnailFile, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return "redirect:/login";
        }
        User user = userService.getUserById(userId);
        if (!user.isAdmin()) {
            return "redirect:/home";
        }

        courtService.createCourt(court.getName(), court.getDescription(), court.getSportType(), court.getAddress(), court.getPricePerHour(), thumbnailFile);
        return "redirect:/home";
    }

    @PostMapping("/courts/{courtId}/delete")
    public String removeCourt(@PathVariable Long courtId, HttpSession session, Model model, RedirectAttributes ra){
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);

        if(userId == null){
            return "redirect:/login";
        }

        User user = userService.getUserById(userId);
        if (!user.isAdmin()) {
            ra.addFlashAttribute("error", "You do not have permission to delete this court.");
            return "redirect:/home";
        }

        try {
            courtService.hardDeleteCourt(courtId);
            ra.addFlashAttribute("success", "Court has been permanently deleted.");
            return "redirect:/home";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error deleting court: " + e.getMessage());
            return "redirect:/home";
        }
    }

    @GetMapping("/courts/{courtId}/schedule") //schedule page for a specific court
    public String showSchedule(@PathVariable Long courtId, Model model, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return "redirect:/login";
        }

        User currentUser = userService.getUserById(userId);
        Court court = courtService.getCourtById(courtId);
        
        if (court == null) {
            return "redirect:/home";
        }

        model.addAttribute("court", court);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("courtId", courtId);
        
        return "schedules";
    }

    @PostMapping("/book")
    public String createBooking(@RequestParam Long courtId, @RequestParam String bookingDate, @RequestParam String startTime, @RequestParam String endTime, HttpSession session, Model model) {
           
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return "redirect:/login";
        }

        try {
            User currentUser = userService.getUserById(userId);
            Court court = courtService.getCourtById(courtId);

            if (court == null) {
                model.addAttribute("error", "Court not found.");
                return "redirect:/home";
            }

            // Parse the date and times
            LocalDate date = LocalDate.parse(bookingDate);
            LocalTime start = LocalTime.parse(startTime);
            LocalTime end = LocalTime.parse(endTime);

            // Create the booking using the service
            bookingService.createBooking(currentUser, court, date, start, end);

            return "redirect:/bookings";

        } catch (Exception e) {
            model.addAttribute("error", "Error creating booking: " + e.getMessage());
            return "redirect:/courts/" + courtId + "/schedule";
        }
    }

    @GetMapping("/bookings")
    public String showMyBookings(Model model, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return "redirect:/login";
        }

        return populateBookingsPage(model, userId, bookingService.getBookingsForUser(userId), "upcoming");
    }

    @GetMapping("/bookings/cancelled")
    public String showCancelledBookings(Model model, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);

        if (userId == null) {
            return "redirect:/login";
        }

        return populateBookingsPage(model, userId, bookingService.getCancelledBookingsForUser(userId), "cancelled");
    }

    private String populateBookingsPage(Model model, Long userId, List<Booking> bookings, String activeTab) {
        User currentUser = userService.getUserById(userId);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isAdmin", currentUser != null && currentUser.isAdmin());
        model.addAttribute("bookings", bookings);
        model.addAttribute("activeTab", activeTab);

        return "my-bookings";
    }

    @PostMapping("/cancel")
    public String handleCancelBooking(@RequestParam Long bookingId,HttpSession session) {

        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        User user = userService.getUserById(userId);

        if (userId == null) {
            return "redirect:/login";
        }

        bookingService.CancelBooking(bookingId, user);

        return "redirect:/bookings";
    }

    
}