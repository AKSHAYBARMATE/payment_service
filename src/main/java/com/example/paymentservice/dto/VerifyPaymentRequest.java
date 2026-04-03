package com.example.paymentservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyPaymentRequest {

    @NotBlank(message = "Order ID is required")
    @Size(max = 64, message = "Order ID must not exceed 64 characters")
    private String orderId;

    @NotBlank(message = "Payment ID is required")
    @Size(max = 64, message = "Payment ID must not exceed 64 characters")
    private String paymentId;

    @NotBlank(message = "Razorpay signature is required")
    @Size(max = 255, message = "Signature must not exceed 255 characters")
    private String razorpaySignature;

    @Size(max = 32, message = "Payment method must not exceed 32 characters")
    private String paymentMethod;
}
