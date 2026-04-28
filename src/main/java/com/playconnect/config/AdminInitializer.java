package com.playconnect.config;

import java.math.BigDecimal;
import java.time.LocalTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.playconnect.entity.Court;
import com.playconnect.entity.User;
import com.playconnect.repository.UserRepository;
import com.playconnect.service.CourtService;
import com.playconnect.service.TimeSlotService;



@Component
public class AdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CourtService courtService;
    private final TimeSlotService timeSlotService;

    public AdminInitializer(UserRepository userRepository,CourtService courtService,TimeSlotService timeSlotService) {
        this.userRepository = userRepository;
        this.courtService = courtService;
        this.timeSlotService = timeSlotService;
    }

    @Override
    public void run(String... args) {

        System.out.println("🔥 INIT RUNNING");

        // 1. Admin
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setEmail("admin@admin.com");
            admin.setPassword("1234");
            admin.setRole("ADMIN");
            admin.setActive(true);
            userRepository.save(admin);
        }

        // 2. Court
        Court court;

        if (courtService.getActiveCourts().isEmpty()) {
            court = courtService.createCourt(
                    "Main Court",
                    "Football Court",
                    "FOOTBALL",
                    "Cairo",
                    new BigDecimal("200")
            );
        } else {
            court = courtService.getActiveCourts().get(0);
        }

        // 3. ALWAYS generate slots
        timeSlotService.generateSlotsForNextDays(
                court,
                LocalTime.of(10, 0),
                LocalTime.of(22, 0)
        );
    }

    
}
