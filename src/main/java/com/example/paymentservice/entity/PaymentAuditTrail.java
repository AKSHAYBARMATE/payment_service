package com.example.paymentservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Entity
@Table(name = "payment_audit_trail")
public class PaymentAuditTrail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    private String razorpaySignature;

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

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String metadata;

    private LocalDateTime subscriptionStartDate;
    private LocalDateTime subscriptionEndDate;

    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
