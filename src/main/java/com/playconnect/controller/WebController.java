package controller;

// This file handles web requests (URLs)

import Entity.*;  
import enums.SportType;  
import Service.*;  

import jakarta.servlet.http.HttpSession;  

import org.springframework.ui.Model;  

import java.util.List;  
import java.time.LocalDate; 
import java.time.LocalTime; 
import org.springframework.stereotype.Controller;  
import org.springframework.web.bind.annotation.*;  
import org.springframework.web.multipart.MultipartFile;  
import org.springframework.web.servlet.mvc.support.RedirectAttributes;  

//This class handles web requests (URLs like /login, /home, etc.)
@Controller  
public class WebController {

    // Session key for storing user ID 
    private static final String SESSION_USER_ID = "SESSION_USER_ID";

    // Services 
    UserService userService;  // Handles user stuff (login, register)
    CourtService courtService;  // Handles court stuff (add, delete, search)
    TimeSlotService timeSlotService;  // Handles time slot stuff (availability)
    BookingService bookingService;  // Handles booking stuff (create, cancel)

    // Constructor
    public WebController(UserService userService, CourtService courtService, TimeSlotService timeSlotService, BookingService bookingService) {
        this.userService = userService;
        this.courtService = courtService;
        this.timeSlotService = timeSlotService;
        this.bookingService = bookingService;
    }

    // HOME & REDIRECT 
    
    @GetMapping("/")  // User goes to http://localhost:8080/
    public String rootRedirect(HttpSession session) {
        if (session.getAttribute(SESSION_USER_ID) != null) {  // If user is logged in
            return "redirect:/home"; 
        }
     // else:
        return "redirect:/login";   
    }

    // LOGIN 
    
    @GetMapping("/login")  // Show login page
    public String showLoginPage(HttpSession session) {
        if(session.getAttribute(SESSION_USER_ID) != null){  // If already logged in
            return "redirect:/home";  
        }
        // else:
        return "login";  
    }

    @PostMapping("/login")  // User submits login form
 // be check lw el infos el gt mein el user mawgoda fel DB wala la
    public String login(@RequestParam String email, @RequestParam String password, HttpSession session, Model model) {
        User user = userService.authenticate(email, password);  

        if (user != null) {  // Login successful
            session.setAttribute(SESSION_USER_ID, user.getId());  // Store user ID in session
            return "redirect:/home";  
        }
        // else:
        model.addAttribute("error", "Invalid email or password.");  // Show error message
        return "login";  // Stay fel login
    }

    // REGISTER 
    
    @GetMapping("/register")  
    public String showRegisterPage() {
        return "register";  
    }

    @PostMapping("/register")  // User submits registration form
    // lw success hay7ot el infos el user 7ataha fel fields bet3atha
    public String register(@RequestParam String firstName, @RequestParam String lastName, @RequestParam String username, @RequestParam String email, @RequestParam String password, Model model) {
        boolean success = userService.registerPlayer(firstName, lastName, username, email, password);

        if (!success) {  // Registration failed (username or email already exists)
            model.addAttribute("error", "Username or email already exists");
            return "register";  
        }
// else:
        return "redirect:/login"; 
    }

    // LOGOUT 
    
    @PostMapping("/logout")  // User clicks logout 
    public String logout(HttpSession session) {
        session.invalidate();  // Clear session (forget user) (BS MESH MEIN EL DB)
        return "redirect:/login";  
    }

    //  HOME/DASHBOARD PAGE 
    
    @GetMapping("/home")  // When user goes to /home URL
    // search word from url w el model be send el data lel html
    public String showCourts(@RequestParam(required = false, defaultValue = "") String search, Model model, HttpSession session) { 
        
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);  // Get user ID from session         
        if (userId == null) {  // Not logged in
            return "redirect:/login";
        }
// bageb el currentuser info mein el db bel id
        User currentUser = userService.getUserById(userId);
        // bab3tha lel html
        model.addAttribute("currentUser", currentUser);
 
        //isAdmin betkon false lw el user exist hab3at el first name lel html lel welcome message
        boolean isAdmin = false;
        if (currentUser != null) {
            model.addAttribute("user", currentUser.getFirstName());
            isAdmin = currentUser.isAdmin(); // check lw el user ADMIN wala la
        }
        model.addAttribute("isAdmin", isAdmin); // Send isAdmin to HTML (so page knows to show/hide admin buttons)

        // Search for courts by name, address, or sport type
        List<Court> courts = courtService.searchActiveCourts(search);
        if (courts == null) {
            courts = List.of();  // Empty list if none found
        }
// else:
        model.addAttribute("courts", courts); // Send list of courts to HTML page
        model.addAttribute("searchTerm", search); // Send search word back to HTML (to show in search box)
        return "home";  // Show home.html
    }

    // ADD COURT (ADMIN ONLY) 
    
    @GetMapping("/courts/new")  // Show form to add new court
    public String showCreateCourtPage(Model model, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return "redirect:/login";
        }
        User user = userService.getUserById(userId);
        if (!user.isAdmin()) {  // Only admins can add courts
            return "redirect:/home";
        }

        model.addAttribute("currentUser", user);
        model.addAttribute("isAdmin", true);
        model.addAttribute("court", new Court());  // Empty court object for form
        model.addAttribute("sportTypes", SportType.values());  // List of sports for dropdown
        return "court-form";  // Show court-form.html
    }

    @PostMapping("/courts")  // User submits new court form
    //  @RequestParam(required = false) MultipartFile thumbnailFile 3lshan yegeb el photo mein el form  (it's optional, not required). 
    // The file will be stored in the variable thumbnailFile
    public String createCourt(@ModelAttribute Court court, @RequestParam(required = false) MultipartFile thumbnailFile, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return "redirect:/login";
        }
        User user = userService.getUserById(userId);
        if (!user.isAdmin()) {
            return "redirect:/home";
        }

        // Save the court to database
        courtService.createCourt(court.getName(), court.getDescription(), court.getSportType(), court.getAddress(), court.getPricePerHour(), thumbnailFile);
        return "redirect:/home"; 
    }

    // DELETE COURT (ADMIN ONLY)
    
    @PostMapping("/courts/{courtId}/delete")  // Admin deletes a court
    // RedirectAttributes lets you show "Success!" messages even after redirecting to another page! 
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
            courtService.hardDeleteCourt(courtId);  // Permanently delete court
            ra.addFlashAttribute("success", "Court has been permanently deleted.");
            return "redirect:/home";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error deleting court: " + e.getMessage());
            return "redirect:/home";
        }
    }

    // SCHEDULE PAGE (BOOKING) 
    
    @GetMapping("/courts/{courtId}/schedule")  // Show schedule for a specific court
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
        
        return "schedules";  // Show schedules.html
    }

    // CREATE BOOKING     
    @PostMapping("/book")  // User books a time slot
    public String createBooking(@RequestParam Long courtId, @RequestParam String bookingDate, @RequestParam String startTime, @RequestParam String endTime, HttpSession session, Model model, RedirectAttributes ra) {
           
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

            // Convert strings to date and time objects
            LocalDate date = LocalDate.parse(bookingDate);
            LocalTime start = LocalTime.parse(startTime);
            LocalTime end = LocalTime.parse(endTime);

            // Create the booking
            bookingService.createBooking(currentUser, court, date, start, end);
            ra.addFlashAttribute("success", "Booking created successfully.");
            return "redirect:/bookings";

        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error creating booking: " + e.getMessage());
            return "redirect:/courts/" + courtId + "/schedule";
        }
    }

    // MY BOOKINGS 
    
    @GetMapping("/bookings")  // Show user's upcoming bookings
    public String showMyBookings(Model model, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return "redirect:/login";
        }

        return populateBookingsPage(model, userId, bookingService.getBookingsForUser(userId), "upcoming");
    }

    @GetMapping("/bookings/cancelled")  // Show user's cancelled bookings
    public String showCancelledBookings(Model model, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return "redirect:/login";
        }

        return populateBookingsPage(model, userId, bookingService.getCancelledBookingsForUser(userId), "cancelled");
    }

    // Helper method to fill the bookings page with data
    private String populateBookingsPage(Model model, Long userId, List<Booking> bookings, String activeTab) {
        User currentUser = userService.getUserById(userId);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isAdmin", currentUser != null && currentUser.isAdmin());
        model.addAttribute("bookings", bookings);
        model.addAttribute("activeTab", activeTab);

        return "my-bookings";  // Show my-bookings.html
    }

    // ========== CANCEL BOOKING ==========
    
    @PostMapping("/cancel")  // User cancels a booking
    public String handleCancelBooking(@RequestParam Long bookingId, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        User user = userService.getUserById(userId);

        if (userId == null) {
            return "redirect:/login";
        }

        bookingService.cancelBooking(bookingId, user);  // Cancel the booking
        return "redirect:/bookings";  // Go back to bookings page
    }
}
