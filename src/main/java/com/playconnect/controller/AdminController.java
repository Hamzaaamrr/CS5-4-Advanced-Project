package com.playconnect.controller;

import com.playconnect.entity.*;
import com.playconnect.service.*;

import jakarta.servlet.http.HttpSession;

import org.springframework.ui.Model;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes; 

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final CourtService courtService;
    private final BookingService bookingService;

    public AdminController(UserService userService, CourtService courtService, BookingService bookingService) {
        this.userService = userService;
        this.courtService = courtService;
        this.bookingService = bookingService;
    }

    private boolean isAdmin(HttpSession session) {
        User user = getCurrentUser(session);

        return user != null && user.isAdmin();
    }

    // Helper method to populate the model with common data for admin views
    private void populateDashboardModel(HttpSession session, Model model) {
        User currentUser = getCurrentUser(session);
        List<User> allUsers = userService.getAllUsers();
        List<Court> allCourts = courtService.getAllCourts();
        List<Booking> allBookings = bookingService.getAllBookings();

        Map<Long, Long> userActiveBookingCounts = new LinkedHashMap<>();
        for (User user : allUsers) {
            userActiveBookingCounts.put(user.getId(), 0L);
        }

        for (Booking booking : allBookings) {
            if (booking.getUser() == null || booking.getUser().getId() == null) {
                continue;
            }
            if (booking.getBookingStatus() == Booking.BookingStatus.CANCELLED) {
                continue;
            }
            Long userId = booking.getUser().getId();
            userActiveBookingCounts.put(userId, userActiveBookingCounts.getOrDefault(userId, 0L) + 1L);
        }

        long activeUsers = 0;
        for (User user : allUsers) {
            if (user.isActive()) {
                activeUsers++;
            }
        }

        long activeCourts = 0;
        for (Court court : allCourts) {
            if (court.isActive()) {
                activeCourts++;
            }
        }

        long confirmedBookings = 0;
        for (Booking booking : allBookings) {
            if (booking.getBookingStatus() == Booking.BookingStatus.CONFIRMED) {
                confirmedBookings++;
            }
        }

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isAdmin", true);
        model.addAttribute("users", allUsers);
        model.addAttribute("userBookingCounts", userActiveBookingCounts);
        model.addAttribute("bookings", allBookings);
        model.addAttribute("totalUsers", allUsers.size());
        model.addAttribute("activeUsers", activeUsers);
        model.addAttribute("totalCourts", allCourts.size());
        model.addAttribute("activeCourts", activeCourts);
        model.addAttribute("totalBookings", allBookings.size());
        model.addAttribute("confirmedBookings", confirmedBookings);
    }

    private User getCurrentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute("SESSION_USER_ID");
        if (userId == null) {
            return null;
        }
        return userService.getUserById(userId);
    }

    //view admin dashboard
    @GetMapping("/dashboard")
    public String dashboard(HttpSession session, Model model, RedirectAttributes ra) {
        if (!isAdmin(session)) {
            ra.addFlashAttribute("error", "Access denied. Admin privileges required.");
            return "redirect:/login";
        }
        populateDashboardModel(session, model);
        
        return "admin/dashboard";
    }

    //view user management part of admin dashboard
    @GetMapping("/users")
    public String manageUsers(HttpSession session, Model model, RedirectAttributes ra) {
        if (!isAdmin(session)) {
            ra.addFlashAttribute("error", "Access denied. Admin privileges required.");
            return "redirect:/login";
        }
        List<User> allUsers = userService.getAllUsers();
        model.addAttribute("currentUser", getCurrentUser(session));
        model.addAttribute("users", allUsers);
        return "admin/users";
    }
    
    
    @PostMapping("/users/delete/{id}")
    public String deleteUser(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) {
            ra.addFlashAttribute("error", "Access denied.");
            return "redirect:/login";
        }
        User currentAdmin = getCurrentUser(session);
        if (currentAdmin.getId().equals(id)) {
            ra.addFlashAttribute("error", "You cannot delete your own account.");
            return "redirect:/admin/users";
        }
        userService.hardDeleteUser(id);
        ra.addFlashAttribute("success", "User has been permanently deleted.");
        return "redirect:/admin/users";
    }
    

    @PostMapping("/courts/delete/{id}")
    public String deleteCourt(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) {
            ra.addFlashAttribute("error", "Access denied.");
            return "redirect:/login";
        }
        courtService.hardDeleteCourt(id);
        ra.addFlashAttribute("success", "Court has been permanently deleted.");
        return "redirect:/admin/courts";
    }

    
<<<<<<< Updated upstream
    // soft delete court: Hide court from users (set active = false)
    @PostMapping("/courts/deactivate/{id}")  // Handles POST requests to hide a court
    public String deactivateCourt(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) {  // If not admin
            ra.addFlashAttribute("error", "Access denied.");  // Show error
            return "redirect:/login";  // Redirect to login
        }
        courtService.deleteCourt(id);  // Set court active = false (soft delete)
        ra.addFlashAttribute("success", "Court has been deactivated.");  // Show success
        return "redirect:/admin/courts";  // Redirect back to courts list
    }
    // restore court: Make hidden court visible again (set active = true)
    @PostMapping("/courts/activate/{id}")  // Handles POST requests to restore a court
    public String activateCourt(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) {  // If not admin
            ra.addFlashAttribute("error", "Access denied.");  // Show error
            return "redirect:/login";  // Redirect to login
        }
        Court court = courtService.getCourtById(id);  // Find court by ID
        if (court != null) {  // If court exists
            court.setActive(true);  // Set active to true
            courtService.updateCourt(court.getId(), court.getName(), court.getDescription(),
                    court.getSportType(), court.getAddress(), court.getPricePerHour());  // Save to database
            ra.addFlashAttribute("success", "Court has been activated.");  // Show success
        }
        return "redirect:/admin/courts";  // Redirect back to courts list
    }

     // hard delete court: remove court from database
    @PostMapping("/courts/delete/{id}")  // Handles POST requests to permanently delete a court
    public String DeleteCourt(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) {  // If not admin
            ra.addFlashAttribute("error", "Access denied.");  // Show error
            return "redirect:/login";  // Redirect to login
        }
        courtService.hardDeleteCourt(id);  // Permanently delete court from database
        ra.addFlashAttribute("success", "Court has been permanently deleted.");  // Show success
        return "redirect:/admin/courts";  // Redirect back to courts list
    }

    // booking management: View all bookings
    @GetMapping("/bookings")  // Handles GET requests to /admin/bookings
    public String manageBookings(HttpSession session, Model model, RedirectAttributes ra) {
        if (!isAdmin(session)) {  // If not admin
            ra.addFlashAttribute("error", "Access denied. Admin privileges required.");  // Show error
            return "redirect:/login";  // Redirect to login
        }
        populateDashboardModel(session, model);
        return "admin/dashboard";  // Return admin/dashboard.html template
    }

    // cancel booking: Admin can cancel any booking
    @PostMapping("/bookings/cancel/{id}")  // Handles POST requests to cancel a booking
=======
    @PostMapping("/bookings/cancel/{id}")
>>>>>>> Stashed changes
    public String cancelBooking(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) {
            ra.addFlashAttribute("error", "Access denied.");
            return "redirect:/login";
        }
<<<<<<< Updated upstream
        User admin = getCurrentUser(session);  // Get the admin user cancelling the booking
        bookingService.CancelBooking(id, admin);  // Cancel the booking (admin can cancel any booking)
        ra.addFlashAttribute("success", "Booking has been cancelled.");  // Show success
        return "redirect:/admin/bookings";  // Redirect back to bookings list
=======
        User admin = getCurrentUser(session);
        bookingService.cancelBooking(id, admin);
        ra.addFlashAttribute("success", "Booking has been cancelled.");
        return "redirect:/admin/bookings";
>>>>>>> Stashed changes
    }
}