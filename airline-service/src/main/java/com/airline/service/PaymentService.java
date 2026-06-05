package com.airline.service;

import com.airline.common.dto.PaymentRequest;
import com.airline.common.dto.PaymentResponse;
import com.airline.common.exception.BadRequestException;
import com.airline.common.exception.ResourceNotFoundException;
import com.airline.common.model.PaymentMethod;
import com.airline.common.model.PaymentStatus;
import com.airline.domain.entity.Booking;
import com.airline.domain.entity.Payment;
import com.airline.domain.repository.BookingRepository;
import com.airline.domain.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final AuditLogService auditLogService;

    /**
     * Simulates payment processing for a booking and updates the booking state.
     *
     * @param request the payment request details
     * @return populated PaymentResponse
     */
    @Transactional
    public PaymentResponse processPayment(PaymentRequest request) {
        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + request.getBookingId()));

        if (paymentRepository.findByBookingId(request.getBookingId()).isPresent()) {
            throw new BadRequestException("Payment already processed for this booking");
        }

        String transactionRef = "TX-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        
        PaymentMethod method = PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase());
        PaymentStatus status = PaymentStatus.SUCCESS;

        Payment payment = Payment.builder()
                .booking(booking)
                .transactionReference(transactionRef)
                .paymentMethod(method)
                .amount(booking.getTotalPrice())
                .paymentStatus(status)
                .build();

        Payment savedPayment = paymentRepository.save(payment);

        // Confirm the booking upon success
        bookingService.confirmBooking(booking.getId(), transactionRef);

        auditLogService.log(booking.getUser(), "PROCESS_PAYMENT", "payments", savedPayment.getId(), "Amount: " + booking.getTotalPrice() + ", Method: " + method);

        return mapToResponse(savedPayment);
    }

    /**
     * Processes refund simulation for a cancelled booking.
     *
     * @param bookingId the booking database ID
     */
    @Transactional
    public void processRefund(Long bookingId) {
        Payment payment = paymentRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment record not found for booking: " + bookingId));

        if (payment.getPaymentStatus() == PaymentStatus.REFUNDED) {
            throw new BadRequestException("Payment is already refunded");
        }

        payment.setPaymentStatus(PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        log.info("Simulated refund processed for booking: {}. Refunded amount: {}", bookingId, payment.getAmount());
        auditLogService.log(payment.getBooking().getUser(), "REFUND_PAYMENT", "payments", payment.getId(), "Amount: " + payment.getAmount());
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .transactionReference(payment.getTransactionReference())
                .status(payment.getPaymentStatus().name())
                .amount(payment.getAmount())
                .timestamp(payment.getCreatedAt())
                .build();
    }
}
