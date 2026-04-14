package com.example.paymentservice.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderResponse {

    private String orderId;
    private String razorpayKey;
    private String userId;
    private String schoolCode;
    private String schoolName;
    private Long planId;
    private String planName;
    private BigDecimal amount;
    private String currency;
    private String paymentStatus;
    private String description;
}
