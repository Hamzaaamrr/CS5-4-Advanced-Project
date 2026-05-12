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

@Controller  // class handles web requests
@RequestMapping("/admin")  // All urls in this controller start with /admin
public class AdminController {

    private final UserService userService;  // Handles user database operations
    private final CourtService courtService;  // Handles court database operations
    private final BookingService bookingService;  // Handles booking database operations

    // Constructor: Spring Boot calls this automatically
    public AdminController(UserService userService, CourtService courtService, BookingService bookingService) {
        this.userService = userService;  // Store UserService for later use
        this.courtService = courtService;  // Store CourtService for later use
        this.bookingService = bookingService;  // Store BookingService for later use
    }

    //method: Check if user is admin
    private boolean isAdmin(HttpSession session) {  // Checks if logged in user is admin
        User user = getCurrentUser(session);  // Get full User object from database

        return user != null && user.isAdmin();  // Return true only if user exists w has admin role
    }

    private void populateDashboardModel(HttpSession session, Model model) {
        User currentUser = getCurrentUser(session);  // Get current admin user
        List<User> allUsers = userService.getAllUsers();  // Get ALL users from database
        List<Court> allCourts = courtService.getAllCourts();  // Get ALL courts from database
        List<Booking> allBookings = bookingService.getAllBookings();  // Get ALL bookings from database

        Map<Long, Long> userActiveBookingCounts = new LinkedHashMap<>();  // Keep user order stable for the dashboard
        for (User user : allUsers) {  // Seed every user with zero active bookings
            userActiveBookingCounts.put(user.getId(), 0L);
        }

        for (Booking booking : allBookings) {  // Count non-cancelled bookings per user
            if (booking.getUser() == null || booking.getUser().getId() == null) {
                continue;
            }
            if (booking.getBookingStatus() == Booking.BookingStatus.CANCELLED) {
                continue;
            }
            Long userId = booking.getUser().getId();
            userActiveBookingCounts.put(userId, userActiveBookingCounts.getOrDefault(userId, 0L) + 1L);
        }

        long activeUsers = 0;  // Counter for active users
        for (User user : allUsers) {  // Loop through each user
            if (user.isActive()) {  // If user is active
                activeUsers++;  // Increase counter
            }
        }

        long activeCourts = 0;  // Counter for active courts
        for (Court court : allCourts) {  // Loop through each court
            if (court.isActive()) {  // If court is active
                activeCourts++;  // Increase counter
            }
        }

        long confirmedBookings = 0;  // Counter for confirmed bookings
        for (Booking booking : allBookings) {  // Loop through each booking
            if (booking.getBookingStatus() == Booking.BookingStatus.CONFIRMED) {  // If booking is confirmed
                confirmedBookings++;  // Increase counter
            }
        }

        model.addAttribute("currentUser", currentUser);  // Send current admin user to HTML
        model.addAttribute("isAdmin", true);  // Flag admin-only template controls
        model.addAttribute("users", allUsers);  // Send list of all users to HTML
        model.addAttribute("userBookingCounts", userActiveBookingCounts);  // Send per-user active booking counts
        model.addAttribute("bookings", allBookings);  // Send list of all bookings to HTML
        model.addAttribute("totalUsers", allUsers.size());  // Send total users count to HTML
        model.addAttribute("activeUsers", activeUsers);  // Send active users count to HTML
        model.addAttribute("totalCourts", allCourts.size());  // Send total courts count to HTML
        model.addAttribute("activeCourts", activeCourts);  // Send active courts count to HTML
        model.addAttribute("totalBookings", allBookings.size());  // Send total bookings count to HTML
        model.addAttribute("confirmedBookings", confirmedBookings);  // Send confirmed bookings count to HTML
    }

    // method: Get current logged in user
    private User getCurrentUser(HttpSession session) {  // Returns the current logged in user
        Long userId = (Long) session.getAttribute("SESSION_USER_ID");  // Get user ID from session
        if (userId == null) {  // If no user ID
            return null;  // Return null (not logged in)
        }
        return userService.getUserById(userId);  // Find and return user from database
    }

    //admin dashboard: Home page for admin
    @GetMapping("/dashboard")  // Handles GET requests to /admin/dashboard
    public String dashboard(HttpSession session, Model model, RedirectAttributes ra) {
        if (!isAdmin(session)) {  // If user is NOT an admin
            ra.addFlashAttribute("error", "Access denied. Admin privileges required.");  // Show error message
            return "redirect:/login";  // Send user back to login page
        }
        populateDashboardModel(session, model);
        
        return "admin/dashboard";  // Return admin/dashboard.html template
    }

    // user management: View all users
    @GetMapping("/users")  // Handles GET requests to /admin/users
    public String manageUsers(HttpSession session, Model model, RedirectAttributes ra) {
        if (!isAdmin(session)) {  // If not admin
            ra.addFlashAttribute("error", "Access denied. Admin privileges required.");  // Show error
            return "redirect:/login";  // Redirect to login
        }
        List<User> allUsers = userService.getAllUsers();  // Get all users from database
        model.addAttribute("currentUser", getCurrentUser(session));  // Send current admin user to HTML
        model.addAttribute("users", allUsers);  // Send list of all users to HTML
        return "admin/users";  // Return admin/users.html
    }
    
    
    // delete user: remove user from database
    @PostMapping("/users/delete/{id}")  // Handles POST requests to permanently delete a user
    public String deleteUser(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) {  // If not admin
            ra.addFlashAttribute("error", "Access denied.");  // Show error
            return "redirect:/login";  // Redirect to login
        }
        User currentAdmin = getCurrentUser(session);  // Get the admin doing this action
        if (currentAdmin.getId().equals(id)) {  // If admin tries to delete themselves
            ra.addFlashAttribute("error", "You cannot delete your own account.");  // Show error
            return "redirect:/admin/users";  // Redirect back
        }
        userService.hardDeleteUser(id);  // Permanently delete user from database
        ra.addFlashAttribute("success", "User has been permanently deleted.");  // Show success
        return "redirect:/admin/users";  // Redirect back to users list
    }

    
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
    public String deleteCourt(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
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
    public String cancelBooking(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (!isAdmin(session)) {  // If not admin
            ra.addFlashAttribute("error", "Access denied.");  // Show error
            return "redirect:/login";  // Redirect to login
        }
        User admin = getCurrentUser(session);  // Get the admin user cancelling the booking
        bookingService.cancelBooking(id, admin);  // Cancel the booking (admin can cancel any booking)
        ra.addFlashAttribute("success", "Booking has been cancelled.");  // Show success
        return "redirect:/admin/bookings";  // Redirect back to bookings list
    }
}