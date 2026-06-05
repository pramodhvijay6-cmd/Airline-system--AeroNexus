package com.airline.service;

import com.airline.common.dto.LoginRequest;
import com.airline.common.dto.RegisterRequest;
import com.airline.common.dto.AuthResponse;
import com.airline.common.model.RoleName;
import com.airline.domain.entity.Role;
import com.airline.domain.entity.User;
import com.airline.domain.repository.RoleRepository;
import com.airline.domain.repository.UserRepository;
import com.airline.service.security.JwtTokenProvider;
import com.airline.service.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Collections;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider tokenProvider;
    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @InjectMocks
    private AuthService authService;

    private User user;
    private Role role;

    @BeforeEach
    public void setup() {
        role = Role.builder().id(1L).name(RoleName.ROLE_CUSTOMER).build();
        user = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .password("encodedPassword")
                .roles(Collections.singleton(role))
                .enabled(true)
                .build();
    }

    @Test
    public void testRegisterSuccess() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPassword("password");
        request.setFirstName("First");
        request.setLastName("Last");

        when(userRepository.existsByUsername(request.getUsername())).thenReturn(false);
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(roleRepository.findByName(RoleName.ROLE_CUSTOMER)).thenReturn(Optional.of(role));
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);

        assertDoesNotThrow(() -> authService.register(request));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    public void testLoginSuccess() {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("password");

        Authentication authentication = mock(Authentication.class);
        UserPrincipal principal = new UserPrincipal(user);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        
        when(tokenProvider.generateAccessToken(principal)).thenReturn("accessToken");
        when(tokenProvider.generateRefreshToken(principal)).thenReturn("refreshToken");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("accessToken", response.getAccessToken());
        assertEquals("refreshToken", response.getRefreshToken());
        assertEquals("testuser", response.getUsername());
    }
}
