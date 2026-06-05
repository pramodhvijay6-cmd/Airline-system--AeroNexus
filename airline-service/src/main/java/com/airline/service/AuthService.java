package com.airline.service;

import com.airline.common.dto.*;
import com.airline.common.exception.BadRequestException;
import com.airline.common.exception.ResourceNotFoundException;
import com.airline.common.model.RoleName;
import com.airline.domain.entity.Role;
import com.airline.domain.entity.User;
import com.airline.domain.repository.RoleRepository;
import com.airline.domain.repository.UserRepository;
import com.airline.service.security.JwtTokenProvider;
import com.airline.service.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service handling authentication operations: login, registration, password resets.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String RESET_PASSWORD_PREFIX = "pwd_reset:";
    private static final String DENYLIST_PREFIX = "jwt_denylist:";

    /**
     * Registers a new user with CUSTOMER role.
     *
     * @param request the registration details
     */
    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already in use");
        }

        Role userRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER)
                .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.ROLE_CUSTOMER).build()));

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phone(request.getPhone())
                .roles(Collections.singleton(userRole))
                .enabled(true)
                .build();

        userRepository.save(user);
        log.info("User registered successfully: {}", user.getUsername());
    }

    /**
     * Authenticates a user and generates access and refresh tokens.
     *
     * @param request the login request DTO
     * @return AuthResponse containing generated tokens
     */
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        String accessToken = tokenProvider.generateAccessToken(userPrincipal);
        String refreshToken = tokenProvider.generateRefreshToken(userPrincipal);

        Set<String> roles = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .username(userPrincipal.getUsername())
                .email(userPrincipal.getUser().getEmail())
                .roles(roles)
                .build();
    }

    /**
     * Refreshes access token using a valid refresh token.
     *
     * @param refreshToken the refresh token string
     * @return AuthResponse with fresh tokens
     */
    public AuthResponse refresh(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new BadRequestException("Invalid refresh token");
        }

        String username = tokenProvider.getUsernameFromToken(refreshToken);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserPrincipal userPrincipal = new UserPrincipal(user);
        String newAccessToken = tokenProvider.generateAccessToken(userPrincipal);
        String newRefreshToken = tokenProvider.generateRefreshToken(userPrincipal);

        Set<String> roles = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .username(username)
                .email(user.getEmail())
                .roles(roles)
                .build();
    }

    /**
     * Invalidates access token on logout by denylisting it in Redis.
     *
     * @param authHeader the authorization header containing Bearer token
     */
    public void logout(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            redisTemplate.opsForValue().set(DENYLIST_PREFIX + token, "true", 24, TimeUnit.HOURS);
            log.info("Token successfully blacklisted on logout");
        }
    }

    /**
     * Generates a password reset token and saves it in Redis with 15-minute TTL.
     *
     * @param request the forgot password request DTO containing email
     */
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));

        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(RESET_PASSWORD_PREFIX + token, user.getEmail(), 15, TimeUnit.MINUTES);

        log.info("Password reset token generated for user: {}", user.getUsername());
    }

    /**
     * Validates reset token and updates the user's password.
     *
     * @param request the reset password request DTO containing token and new password
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = redisTemplate.opsForValue().get(RESET_PASSWORD_PREFIX + request.getToken());
        if (email == null) {
            throw new BadRequestException("Invalid or expired password reset token");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        redisTemplate.delete(RESET_PASSWORD_PREFIX + request.getToken());
        log.info("Password successfully reset for user: {}", user.getUsername());
    }

    /**
     * Checks if a token is denylisted.
     *
     * @param token the access token string
     * @return true if blacklisted, false otherwise
     */
    public boolean isTokenDenylisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(DENYLIST_PREFIX + token));
    }
}
