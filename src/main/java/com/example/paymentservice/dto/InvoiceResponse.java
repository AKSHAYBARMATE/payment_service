package com.example.paymentservice.dto;

import com.example.paymentservice.enums.InvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {

    private String invoiceNumber;
    private String orderId;
    private String paymentId;
    private String schoolCode;
    private String schoolName;
    private String planName;
    private String billingPeriodLabel;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private BigDecimal taxableAmount;
    private BigDecimal cgstAmount;
    private BigDecimal sgstAmount;
    private BigDecimal totalAmount;
    private String currency;
    private InvoiceStatus status;
    private String downloadUrl;
}
