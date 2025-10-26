package com.n1str.booking;

import com.n1str.booking.entity.User;
import com.n1str.booking.repository.UserRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.event.EventListener;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableRetry
public class BookingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingServiceApplication.class, args);
    }

    public BookingServiceApplication() {
    }

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setUserRepository(UserRepository userRepository) { this.userRepository = userRepository; }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setPasswordEncoder(PasswordEncoder passwordEncoder) { this.passwordEncoder = passwordEncoder; }

    @EventListener(ApplicationReadyEvent.class)
    public void ensureAdminUser() {
        if (userRepository == null || passwordEncoder == null) {
            return;
        }
        userRepository.findByUsername("admin").ifPresentOrElse(existing -> {
            if (!passwordEncoder.matches("admin123", existing.getPassword()) || !"ADMIN".equals(existing.getRole())) {
                existing.setPassword(passwordEncoder.encode("admin123"));
                existing.setRole("ADMIN");
                if (existing.getCreatedAt() == null) {
                    existing.setCreatedAt(LocalDateTime.now());
                }
                userRepository.save(existing);
            }
        }, () -> {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setRole("ADMIN");
            admin.setCreatedAt(LocalDateTime.now());
            userRepository.save(admin);
        });
    }
}

