package com.moviebooking.ticket_booking.auth.service;

import com.moviebooking.ticket_booking.auth.entity.Role;
import com.moviebooking.ticket_booking.auth.entity.User;
import com.moviebooking.ticket_booking.auth.exception.InvalidCredentialsException;
import com.moviebooking.ticket_booking.auth.repository.UserRepository;
import com.moviebooking.ticket_booking.common.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registration and credential verification. Token issuance is orchestrated in the request handler. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User register(String name, String email, String phone, String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPhone(phone);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(Role.CUSTOMER);
        User saved = userRepository.save(user);
        log.info("Registered customer {} (id={})", saved.getEmail(), saved.getId());
        return saved;
    }

    @Transactional(readOnly = true)
    public User authenticate(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return user;
    }
}
