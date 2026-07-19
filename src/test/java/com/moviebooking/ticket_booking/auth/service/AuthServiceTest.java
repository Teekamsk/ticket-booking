package com.moviebooking.ticket_booking.auth.service;

import com.moviebooking.ticket_booking.auth.entity.Role;
import com.moviebooking.ticket_booking.auth.entity.User;
import com.moviebooking.ticket_booking.auth.exception.InvalidCredentialsException;
import com.moviebooking.ticket_booking.auth.repository.UserRepository;
import com.moviebooking.ticket_booking.common.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_newEmail_persistsCustomerWithEncodedPassword() {
        when(userRepository.existsByEmail("jane@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret123")).thenReturn("hashed");
        when(userRepository.save(org.mockito.ArgumentMatchers.any(User.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        User result = authService.register("Jane", "jane@example.com", "9998887776", "Secret123");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getPasswordHash()).isEqualTo("hashed");
        assertThat(saved.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(result.getEmail()).isEqualTo("jane@example.com");
    }

    @Test
    void register_duplicateEmail_throwsConflict() {
        when(userRepository.existsByEmail("dup@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register("Dup", "dup@example.com", null, "Secret123"))
                .isInstanceOf(ConflictException.class);
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void authenticate_validCredentials_returnsUser() {
        User user = new User();
        user.setEmail("jane@example.com");
        user.setPasswordHash("hashed");
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Secret123", "hashed")).thenReturn(true);

        assertThat(authService.authenticate("jane@example.com", "Secret123")).isSameAs(user);
    }

    @Test
    void authenticate_unknownEmail_throwsInvalidCredentials() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.authenticate("nobody@example.com", "Secret123"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void authenticate_wrongPassword_throwsInvalidCredentials() {
        User user = new User();
        user.setPasswordHash("hashed");
        when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(user));
        lenient().when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.authenticate("jane@example.com", "wrong"))
                .isInstanceOf(InvalidCredentialsException.class);
    }
}
