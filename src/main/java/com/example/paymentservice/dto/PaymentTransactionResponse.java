package com.example.paymentservice.dto;

import com.example.paymentservice.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTransactionResponse {

    private Long id;
    private String orderId;
    private String paymentId;
    private String userId;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus paymentStatus;
    private String paymentMethod;
    private String description;
    private String metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
