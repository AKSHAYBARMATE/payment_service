package com.example.paymentservice.dto;

import com.example.paymentservice.enums.PaymentStatus;
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
    private BigDecimal amount;
    private String currency;
    private PaymentStatus paymentStatus;
    private String description;
}
