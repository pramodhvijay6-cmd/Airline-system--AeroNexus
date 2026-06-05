package com.airline.service;

import com.airline.common.model.NotificationStatus;
import com.airline.domain.entity.Notification;
import com.airline.domain.entity.User;
import com.airline.domain.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    /**
     * Asynchronously sends an email notification and records it in the database.
     *
     * @param user the recipient User
     * @param title the email subject line
     * @param message the email body content
     */
    @Async
    public void sendEmailNotification(User user, String title, String message) {
        log.info("Sending email notification to {}: [{}] - {}", user.getEmail(), title, message);

        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .channel("EMAIL")
                .status(NotificationStatus.PENDING)
                .build();

        try {
            if (mailSender != null) {
                SimpleMailMessage mailMessage = new SimpleMailMessage();
                mailMessage.setTo(user.getEmail());
                mailMessage.setSubject(title);
                mailMessage.setText(message);
                mailSender.send(mailMessage);
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
            } else {
                log.warn("JavaMailSender bean is not available. Simulating email dispatch.");
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
            }
        } catch (Exception e) {
            log.error("Failed to send email to user. Logging error: {}", e.getMessage());
            notification.setStatus(NotificationStatus.FAILED);
            notification.setSentAt(LocalDateTime.now());
        }

        notificationRepository.save(notification);
    }
}
