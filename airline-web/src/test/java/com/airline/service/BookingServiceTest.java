package com.airline.service;

import com.airline.common.dto.BookingRequest;
import com.airline.common.dto.BookingResponse;
import com.airline.common.dto.PassengerDto;
import com.airline.common.model.BookingStatus;
import com.airline.domain.entity.*;
import com.airline.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookingServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private FlightRepository flightRepository;
    @Mock private UserRepository userRepository;
    @Mock private PassengerRepository passengerRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private CouponRepository couponRepository;
    @Mock private RedisTemplate<String, String> redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private NotificationService notificationService;
    @Mock private LoyaltyService loyaltyService;
    @Mock private AuditLogService auditLogService;
    @Mock private DynamicPricingService dynamicPricingService;

    @InjectMocks
    private BookingService bookingService;

    private User user;
    private Flight flight;
    private Booking booking;

    @BeforeEach
    public void setup() {
        user = User.builder().id(1L).username("testuser").email("test@example.com").build();
        Aircraft aircraft = Aircraft.builder().capacity(100).model("B737").build();
        Route route = Route.builder().origin("JFK").destination("LAX").build();
        flight = Flight.builder()
                .id(1L)
                .flightNumber("SF-100")
                .aircraft(aircraft)
                .route(route)
                .departureTime(LocalDateTime.now().plusDays(5))
                .basePrice(BigDecimal.valueOf(200.00))
                .status(com.airline.common.model.FlightStatus.SCHEDULED)
                .build();
        booking = Booking.builder()
                .id(1L)
                .bookingReference("ABC123")
                .bookingStatus(BookingStatus.PENDING)
                .totalPrice(BigDecimal.valueOf(200.00))
                .flight(flight)
                .user(user)
                .build();
    }

    @Test
    public void testCreateBookingSuccess() {
        BookingRequest request = new BookingRequest();
        request.setFlightId(1L);
        PassengerDto pDto = new PassengerDto();
        pDto.setFirstName("John");
        pDto.setLastName("Doe");
        pDto.setSeatNumber("12A");
        request.setPassengers(Collections.singletonList(pDto));

        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(flightRepository.findById(1L)).thenReturn(Optional.of(flight));
        
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        when(dynamicPricingService.calculatePrice(any(BigDecimal.class), any(LocalDateTime.class), anyInt(), anyInt()))
                .thenReturn(BigDecimal.valueOf(200.00));

        when(bookingRepository.save(any(Booking.class))).thenReturn(booking);

        BookingResponse response = bookingService.createBooking("testuser", request);

        assertNotNull(response);
        assertEquals("PENDING", response.getStatus());
        assertEquals(BigDecimal.valueOf(200.00), response.getTotalPrice());
        verify(bookingRepository, times(1)).save(any(Booking.class));
    }
}
