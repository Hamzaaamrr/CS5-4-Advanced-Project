package com.playconnect.controller;

import com.playconnect.entity.*;
import com.playconnect.service.*;

import org.springframework.ui.Model;

// import com.playconnect.repository.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class WebController {
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

    @GetMapping("/login")
    public String showLoginPage() {
        return "login"; // Return to login view
    }


    @PostMapping("/login")
    public String login(@RequestParam String email, @RequestParam String password, Model model) {
        User user = userService.authenticate(email, password);
        if(user != null){
            model.addAttribute("user", user.getFirstName());
            return "home"; // Redirect to home page on successful login
        }
        return "login"; // Return to login page on failed login
    }

    @PostMapping("/register")
    public String register(@RequestParam String firstName, @RequestParam String lastName, @RequestParam String username, @RequestParam String email, @RequestParam String password, Model model) {
        boolean success = userService.registerPlayer(firstName, lastName, username, email, password);
        if(success){
            return "redirect:/login"; // Redirect to login page on successful registration
        }
        model.addAttribute("error", "Username or email already exists.");
        return "register"; // Return to registration page on failed registration
    }

    @GetMapping("/home")
    public String showHomePage(Model model) {
        model.addAttribute("user", "John Doe"); // Example user name
        return "home";
    }
    
    @GetMapping("/register")
    public String showRegisterPage() {
        return "register"; // Return the name of the registration view 
    }

}
