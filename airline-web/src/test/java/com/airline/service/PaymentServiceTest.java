package com.airline.service;

import com.airline.common.dto.PaymentRequest;
import com.airline.common.dto.PaymentResponse;
import com.airline.common.exception.BadRequestException;
import com.airline.common.exception.ResourceNotFoundException;
import com.airline.common.model.PaymentMethod;
import com.airline.common.model.PaymentStatus;
import com.airline.domain.entity.Booking;
import com.airline.domain.entity.Payment;
import com.airline.domain.entity.User;
import com.airline.domain.repository.BookingRepository;
import com.airline.domain.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingService bookingService;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private PaymentService paymentService;

    private User user;
    private Booking booking;
    private Payment payment;

    @BeforeEach
    public void setup() {
        user = User.builder().id(1L).username("testuser").email("test@example.com").build();
        booking = Booking.builder()
                .id(1L)
                .user(user)
                .totalPrice(new BigDecimal("150.00"))
                .bookingReference("PNR123")
                .build();
        payment = Payment.builder()
                .id(1L)
                .booking(booking)
                .amount(new BigDecimal("150.00"))
                .paymentMethod(PaymentMethod.CREDIT_CARD)
                .paymentStatus(PaymentStatus.SUCCESS)
                .transactionReference("TX-1234567890")
                .build();
    }

    @Test
    public void testProcessPaymentSuccess() {
        PaymentRequest request = new PaymentRequest();
        request.setBookingId(1L);
        request.setPaymentMethod("CREDIT_CARD");

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        PaymentResponse response = paymentService.processPayment(request);

        assertNotNull(response);
        assertEquals("SUCCESS", response.getStatus());
        assertEquals(new BigDecimal("150.00"), response.getAmount());
        verify(bookingService, times(1)).confirmBooking(eq(1L), anyString());
        verify(auditLogService, times(1)).log(eq(user), eq("PROCESS_PAYMENT"), eq("payments"), eq(1L), anyString());
    }

    @Test
    public void testProcessPaymentBookingNotFound() {
        PaymentRequest request = new PaymentRequest();
        request.setBookingId(2L);
        request.setPaymentMethod("CREDIT_CARD");

        when(bookingRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.processPayment(request));
    }

    @Test
    public void testProcessPaymentAlreadyPaid() {
        PaymentRequest request = new PaymentRequest();
        request.setBookingId(1L);
        request.setPaymentMethod("CREDIT_CARD");

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(booking));
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.of(payment));

        assertThrows(BadRequestException.class, () -> paymentService.processPayment(request));
    }

    @Test
    public void testProcessRefundSuccess() {
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any(Payment.class))).thenReturn(payment);

        assertDoesNotThrow(() -> paymentService.processRefund(1L));
        assertEquals(PaymentStatus.REFUNDED, payment.getPaymentStatus());
        verify(paymentRepository, times(1)).save(payment);
        verify(auditLogService, times(1)).log(eq(user), eq("REFUND_PAYMENT"), eq("payments"), eq(1L), anyString());
    }

    @Test
    public void testProcessRefundNotFound() {
        when(paymentRepository.findByBookingId(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> paymentService.processRefund(2L));
    }

    @Test
    public void testProcessRefundAlreadyRefunded() {
        payment.setPaymentStatus(PaymentStatus.REFUNDED);
        when(paymentRepository.findByBookingId(1L)).thenReturn(Optional.of(payment));

        assertThrows(BadRequestException.class, () -> paymentService.processRefund(1L));
    }
}
