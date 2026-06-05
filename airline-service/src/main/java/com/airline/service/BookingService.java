package com.airline.service;

import com.airline.common.dto.*;
import com.airline.common.exception.BadRequestException;
import com.airline.common.exception.ResourceNotFoundException;
import com.airline.common.model.BookingStatus;
import com.airline.common.model.DiscountType;
import com.airline.common.model.FlightStatus;
import com.airline.domain.entity.*;
import com.airline.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service orchestrating seat locking, PNR creation, waitlists, transactions, and ticket generation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    private final BookingRepository bookingRepository;
    private final FlightRepository flightRepository;
    private final UserRepository userRepository;
    private final PassengerRepository passengerRepository;
    private final TicketRepository ticketRepository;
    private final CouponRepository couponRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final NotificationService notificationService;
    private final LoyaltyService loyaltyService;
    private final AuditLogService auditLogService;
    private final DynamicPricingService dynamicPricingService;

    private static final String SEAT_LOCK_PREFIX = "seat_lock:";
    private static final String WAITLIST_PREFIX = "waitlist:";
    private static final SecureRandom random = new SecureRandom();

    /**
     * Initiates a booking transaction: locks seats in Redis and saves a PENDING booking.
     *
     * @param username the customer's username
     * @param request  the booking request details
     * @return populated BookingResponse
     */
    @Transactional
    public BookingResponse createBooking(String username, BookingRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
        Flight flight = flightRepository.findById(request.getFlightId())
                .orElseThrow(() -> new ResourceNotFoundException("Flight not found with id: " + request.getFlightId()));

        if (flight.getStatus() == FlightStatus.CANCELLED) {
            throw new BadRequestException("Cannot book seats on a cancelled flight.");
        }

        int capacity = flight.getAircraft().getCapacity();
        long confirmedBookingsCount = bookingRepository.findByFlightIdAndBookingStatus(flight.getId(), BookingStatus.CONFIRMED).size();
        int availableSeats = Math.max(0, capacity - (int) confirmedBookingsCount);

        boolean waitlisted = false;
        if (availableSeats < request.getPassengers().size()) {
            waitlisted = true;
            log.info("Flight is full. Booking will be placed on the waitlist.");
        }

        // 1. Acquire Redis seat locks if not waitlisted
        List<String> lockedKeys = new ArrayList<>();
        if (!waitlisted) {
            for (PassengerDto p : request.getPassengers()) {
                String lockKey = SEAT_LOCK_PREFIX + flight.getId() + ":" + p.getSeatNumber();
                Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, user.getId().toString(), 10, TimeUnit.MINUTES);
                if (Boolean.FALSE.equals(acquired)) {
                    // Release all previously locked seats in this request
                    lockedKeys.forEach(redisTemplate::delete);
                    throw new BadRequestException("Seat " + p.getSeatNumber() + " is already locked or booked. Please try another seat.");
                }
                lockedKeys.add(lockKey);
            }
        }

        // 2. Calculate Pricing
        BigDecimal flightPrice = dynamicPricingService.calculatePrice(
                flight.getBasePrice(),
                flight.getDepartureTime(),
                capacity,
                (int) confirmedBookingsCount
        );

        BigDecimal subtotal = flightPrice.multiply(BigDecimal.valueOf(request.getPassengers().size()));
        BigDecimal discount = BigDecimal.ZERO;

        Coupon coupon = null;
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            coupon = couponRepository.findByCode(request.getCouponCode())
                    .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with code: " + request.getCouponCode()));

            if (coupon.getExpirationDate() != null && coupon.getExpirationDate().isBefore(LocalDateTime.now())) {
                throw new BadRequestException("Coupon has expired");
            }
            if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
                throw new BadRequestException("Coupon usage limit exceeded");
            }
            if (subtotal.compareTo(coupon.getMinOrderValue()) < 0) {
                throw new BadRequestException("Minimum order value for coupon not met");
            }

            if (coupon.getDiscountType() == DiscountType.PERCENTAGE) {
                discount = subtotal.multiply(coupon.getDiscountValue().divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP));
                if (coupon.getMaxDiscount() != null && discount.compareTo(coupon.getMaxDiscount()) > 0) {
                    discount = coupon.getMaxDiscount();
                }
            } else {
                discount = coupon.getDiscountValue();
            }
        }

        BigDecimal totalPrice = subtotal.subtract(discount).max(BigDecimal.ZERO);

        // 3. Save Booking
        String pnr = generatePNR();
        Booking booking = Booking.builder()
                .user(user)
                .flight(flight)
                .bookingReference(pnr)
                .bookingStatus(BookingStatus.PENDING)
                .totalPrice(totalPrice)
                .coupon(coupon)
                .build();

        Booking savedBooking = bookingRepository.save(booking);

        // 4. Save Passengers
        List<Passenger> passengers = request.getPassengers().stream().map(p -> Passenger.builder()
                .booking(savedBooking)
                .firstName(p.getFirstName())
                .lastName(p.getLastName())
                .email(p.getEmail())
                .passportNumber(p.getPassportNumber())
                .seatNumber(p.getSeatNumber())
                .build()).collect(Collectors.toList());

        passengerRepository.saveAll(passengers);

        // 5. Place in waitlist if full
        if (waitlisted) {
            redisTemplate.opsForList().rightPush(WAITLIST_PREFIX + flight.getId(), savedBooking.getId().toString());
            log.info("Booking {} pushed to waitlist queue", savedBooking.getId());
        }

        auditLogService.log(user, "CREATE_BOOKING", "bookings", savedBooking.getId(), "PNR: " + pnr + ", Waitlisted: " + waitlisted);

        return mapToResponse(savedBooking, passengers);
    }

    /**
     * Confirms booking, generates tickets, processes loyalty points, and releases seat locks.
     *
     * @param bookingId      the booking database ID
     * @param transactionRef simulated payment transaction reference
     */
    @Transactional
    public void confirmBooking(Long bookingId, String transactionRef) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (booking.getBookingStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Booking cannot be confirmed from status: " + booking.getBookingStatus());
        }

        booking.setBookingStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);

        // Increment coupon usage count
        if (booking.getCoupon() != null) {
            Coupon coupon = booking.getCoupon();
            coupon.setUsedCount(coupon.getUsedCount() + 1);
            couponRepository.save(coupon);
        }

        // Generate tickets
        List<Passenger> passengers = passengerRepository.findByBookingId(bookingId);
        for (Passenger passenger : passengers) {
            String ticketNum = "TK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            ticketRepository.save(Ticket.builder()
                    .passenger(passenger)
                    .ticketNumber(ticketNum)
                    .ticketStatus("ACTIVE")
                    .build());

            // Release seat lock in Redis
            String lockKey = SEAT_LOCK_PREFIX + booking.getFlight().getId() + ":" + passenger.getSeatNumber();
            redisTemplate.delete(lockKey);
        }

        // Accrue loyalty points
        loyaltyService.accruePoints(booking.getUser(), booking.getTotalPrice());

        // Send Async email notification
        notificationService.sendEmailNotification(
                booking.getUser(),
                "Flight Booking Confirmed - PNR: " + booking.getBookingReference(),
                "Dear " + booking.getUser().getFirstName() + ", your flight " 
                + booking.getFlight().getFlightNumber() + " is confirmed. PNR: " 
                + booking.getBookingReference() + "."
        );

        auditLogService.log(booking.getUser(), "CONFIRM_BOOKING", "bookings", booking.getId(), "PNR: " + booking.getBookingReference() + ", TxRef: " + transactionRef);
    }

    /**
     * Cancels a booking, releases seat locks, processes waitlisted passengers, and triggers refunds.
     *
     * @param bookingId the booking database ID
     */
    @Transactional
    public void cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Booking is already cancelled");
        }

        booking.setBookingStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        // Release seat locks if any
        List<Passenger> passengers = passengerRepository.findByBookingId(bookingId);
        for (Passenger passenger : passengers) {
            String lockKey = SEAT_LOCK_PREFIX + booking.getFlight().getId() + ":" + passenger.getSeatNumber();
            redisTemplate.delete(lockKey);

            // Void tickets
            ticketRepository.findByPassengerId(passenger.getId()).ifPresent(t -> {
                t.setTicketStatus("VOID");
                ticketRepository.save(t);
            });
        }

        // Process waitlist: Check if there's someone waiting for this flight
        String waitlistKey = WAITLIST_PREFIX + booking.getFlight().getId();
        String nextBookingIdStr = redisTemplate.opsForList().leftPop(waitlistKey);
        if (nextBookingIdStr != null) {
            Long nextBookingId = Long.parseLong(nextBookingIdStr);
            bookingRepository.findById(nextBookingId).ifPresent(nextBooking -> {
                log.info("Auto-notifying waitlisted booking: {} to proceed with payment", nextBooking.getBookingReference());
                notificationService.sendEmailNotification(
                        nextBooking.getUser(),
                        "Waitlist Cleared - PNR: " + nextBooking.getBookingReference(),
                        "Good news! Your waitlisted flight " + nextBooking.getFlight().getFlightNumber() 
                        + " has cleared. Please log in and complete your payment within 15 minutes."
                );
            });
        }

        // Async cancellation notification
        notificationService.sendEmailNotification(
                booking.getUser(),
                "Flight Booking Cancelled - PNR: " + booking.getBookingReference(),
                "Dear " + booking.getUser().getFirstName() + ", your flight booking " 
                + booking.getBookingReference() + " has been cancelled successfully."
        );

        auditLogService.log(booking.getUser(), "CANCEL_BOOKING", "bookings", booking.getId(), "PNR: " + booking.getBookingReference());
    }

    /**
     * Lists all bookings associated with a specific user.
     *
     * @param username the customer's username
     * @return a list of BookingResponse DTOs
     */
    @Transactional(readOnly = true)
    public List<BookingResponse> getUserBookings(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return bookingRepository.findByUser(user).stream()
                .map(b -> mapToResponse(b, passengerRepository.findByBookingId(b.getId())))
                .collect(Collectors.toList());
    }

    private String generatePNR() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private BookingResponse mapToResponse(Booking booking, List<Passenger> passengers) {
        List<PassengerResponseDto> passengerDtos = passengers.stream().map(p -> {
            String ticketNum = ticketRepository.findByPassengerId(p.getId())
                    .map(Ticket::getTicketNumber)
                    .orElse(null);

            return PassengerResponseDto.builder()
                    .id(p.getId())
                    .firstName(p.getFirstName())
                    .lastName(p.getLastName())
                    .email(p.getEmail())
                    .passportNumber(p.getPassportNumber())
                    .seatNumber(p.getSeatNumber())
                    .ticketNumber(ticketNum)
                    .build();
        }).collect(Collectors.toList());

        FlightResponse flightResponse = FlightResponse.builder()
                .id(booking.getFlight().getId())
                .flightNumber(booking.getFlight().getFlightNumber())
                .origin(booking.getFlight().getRoute().getOrigin())
                .destination(booking.getFlight().getRoute().getDestination())
                .departureTime(booking.getFlight().getDepartureTime())
                .arrivalTime(booking.getFlight().getArrivalTime())
                .status(booking.getFlight().getStatus().name())
                .basePrice(booking.getFlight().getBasePrice())
                .currentPrice(booking.getFlight().getBasePrice())
                .build();

        return BookingResponse.builder()
                .id(booking.getId())
                .bookingReference(booking.getBookingReference())
                .status(booking.getBookingStatus().name())
                .totalPrice(booking.getTotalPrice())
                .flight(flightResponse)
                .passengers(passengerDtos)
                .bookingTime(booking.getBookingTime())
                .build();
    }

    /**
     * Retrieves seat numbers that are already confirmed for a flight.
     *
     * @param flightId database ID of the flight
     * @return list of occupied seat numbers
     */
    @Transactional(readOnly = true)
    public List<String> getOccupiedSeats(Long flightId) {
        return passengerRepository.findOccupiedSeatsByFlightId(flightId);
    }
}

