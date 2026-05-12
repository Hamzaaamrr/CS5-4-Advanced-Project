package com.playconnect.service;

import com.playconnect.entity.Booking;
import com.playconnect.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendBookingEmail(User user, Booking booking) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("playyconnectt@gmail.com");
            message.setTo(user.getEmail());
            message.setSubject("Booking Confirmation");
            message.setText(
                    "Dear " + user.getFullName() + ",\n\n"
                            + "Your booking has been confirmed.\n"
                            + "Booking ID: " + booking.getId() + "\n"
                            + "Court: " + booking.getCourt().getName() + "\n"
                            + "Total Price: " + booking.getTotalPrice() + "\n\n"
                            + "Thank you for booking with PlayConnect ❤️"
            );
            mailSender.send(message);
            System.out.println("EMAIL SENT SUCCESSFULLY");
        } catch (Exception e) {
            System.out.println("EMAIL FAILED");
            e.printStackTrace();
        }
    }

    public void sendCancellationEmail(User user, Booking booking) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("playyconnectt@gmail.com");
            message.setTo(user.getEmail());
            message.setSubject("Booking Cancellation");
            message.setText(
                    "Dear " + user.getFullName() + ",\n\n"
                            + "Your booking has been cancelled.\n"
                            + "Booking ID: " + booking.getId() + "\n"
                            + "Court: " + booking.getCourt().getName() + "\n"
                            + "Status: CANCELLED\n\n"
                            + "We hope to see you again soon at PlayConnect ❤️"
            );
            mailSender.send(message);
            System.out.println("CANCELLATION EMAIL SENT SUCCESSFULLY");
        } catch (Exception e) {
            System.out.println("CANCELLATION EMAIL FAILED");
            e.printStackTrace();
        }
    }
}