package com.example.paymentservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
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
public class CreateOrderRequest {

    @NotBlank(message = "User ID is required")
    @Size(max = 64, message = "User ID must not exceed 64 characters")
    private String userId;

    @NotBlank(message = "School code is required")
    @Size(max = 64, message = "School code must not exceed 64 characters")
    private String schoolCode;

    @NotNull(message = "Plan ID is required")
    private Long planId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "1.00", message = "Amount must be at least 1.00")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(max = 8, message = "Currency must not exceed 8 characters")
    private String currency;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    @Size(max = 4000, message = "Metadata must not exceed 4000 characters")
    private String metadata;
}
