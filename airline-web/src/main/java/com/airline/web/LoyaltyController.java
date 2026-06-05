package com.airline.web;

import com.airline.common.dto.ApiResponse;
import com.airline.domain.entity.LoyaltyPoint;
import com.airline.domain.entity.LoyaltyTransaction;
import com.airline.domain.repository.LoyaltyPointRepository;
import com.airline.domain.repository.LoyaltyTransactionRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/loyalty")
@RequiredArgsConstructor
public class LoyaltyController {

    private final LoyaltyPointRepository loyaltyPointRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final com.airline.domain.repository.UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLoyaltyDetails(
            @AuthenticationPrincipal UserDetails userDetails, HttpServletRequest servletRequest) {
        
        com.airline.domain.entity.User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new com.airline.common.exception.ResourceNotFoundException("User not found"));

        LoyaltyPoint loyaltyPoint = loyaltyPointRepository.findByUserId(user.getId())
                .orElseGet(() -> loyaltyPointRepository.save(
                        LoyaltyPoint.builder()
                                .user(user)
                                .currentBalance(0)
                                .tier(com.airline.common.model.LoyaltyTier.BRONZE)
                                .build()
                ));

        List<LoyaltyTransaction> transactions = loyaltyTransactionRepository
                .findByLoyaltyPointIdOrderByTransactionDateDesc(loyaltyPoint.getId());

        Map<String, Object> details = new HashMap<>();
        details.put("currentBalance", loyaltyPoint.getCurrentBalance());
        details.put("tier", loyaltyPoint.getTier().name());
        details.put("transactions", transactions);

        return ResponseEntity.ok(ApiResponse.success("Loyalty points and tier details retrieved", details, servletRequest.getRequestURI()));
    }
}
