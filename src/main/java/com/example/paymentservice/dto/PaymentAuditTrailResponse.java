package com.example.paymentservice.dto;

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
public class PaymentAuditTrailResponse {

    private Long id;
    private Long schoolId;
    private String schoolCode;
    private String schoolName;
    private Long planId;
    private String planName;
    private Integer planDurationInMonths;
    private String initiatedByUserId;
    private String orderId;
    private String paymentId;
    private BigDecimal amount;
    private BigDecimal taxableAmount;
    private BigDecimal cgstAmount;
    private BigDecimal sgstAmount;
    private BigDecimal totalAmount;
    private String currency;
    private String paymentStatus;
    private String paymentMethod;
    private String auditAction;
    private String invoiceNumber;
    private String description;
    private String metadata;
    private String downloadUrl;
    private LocalDateTime subscriptionStartDate;
    private LocalDateTime subscriptionEndDate;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
