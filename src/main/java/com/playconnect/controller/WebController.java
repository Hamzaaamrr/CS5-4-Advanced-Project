package com.playconnect.controller;

import com.playconnect.entity.*;
import com.playconnect.enums.SportType;
import com.playconnect.service.*;

import jakarta.servlet.http.HttpSession;

import org.springframework.ui.Model;

import java.security.Principal;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class WebController {

    private static final String SESSION_USER_ID = "SESSION_USER_ID";
    private static final String SESSION_CART = "SESSION_CART";

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
    public String login(@RequestParam String email, @RequestParam String password, HttpSession session) {
        User user = userService.authenticate(email, password);
        if (user != null) {
            if ("admin".equals(username) && "admin".equals(password)) {
                user.setRole("ADMIN");
            }
            session.setAttribute(SESSION_USER_ID, user.getId());
            return "redirect:/home";
        }
        return "login";
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
    public String showCourts(Model model, HttpSession session) {
            Long userId = (Long) session.getAttribute(SESSION_USER_ID);
            User currentUser = null;

            if (userId != null) {
                currentUser = userService.getUserById(userId);
            }
            model.addAttribute("currentUser", currentUser);

            boolean isAdmin = false;
            if (currentUser != null) {
                model.addAttribute("user", currentUser.getFirstName());
                isAdmin = currentUser.isAdmin();
            }
            model.addAttribute("isAdmin", isAdmin);

            List<Court> courts = courtService.getActiveCourts();
            if (courts == null) {
                courts = List.of();
            }

            model.addAttribute("courts", courts);
            model.addAttribute("cartCount", 0);
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

        model.addAttribute("court", new Court());
        model.addAttribute("sportTypes", SportType.values());
        return "court-form";
    }

    @PostMapping("/courts")
    public String createCourt(@ModelAttribute Court court, HttpSession session) {
        Long userId = (Long) session.getAttribute(SESSION_USER_ID);
        if (userId == null) {
            return "redirect:/login";
        }
        User user = userService.getUserById(userId);
        if (!user.isAdmin()) {
            return "redirect:/home";
        }

        courtService.createCourt(court.getName(), court.getDescription(), court.getSportType(), court.getAddress(), court.getPricePerHour());
        return "redirect:/home";
    }
    
}