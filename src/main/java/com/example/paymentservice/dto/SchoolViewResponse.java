package com.example.paymentservice.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchoolViewResponse {

    private Long id;
    private String name;
    private String schoolCode;
    private String board;
    private String medium;
    private String email;
    private String phone;
    private String alternatePhone;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String pincode;
    private String country;
    private String adminName;
    private String adminEmail;
    private String adminUsername;
    private String schemaName;
    private String status;
    private Long planId;
    private String planName;
    private Integer planDurationInMonths;
    private String planStatus;
    private LocalDateTime licenseStartDate;
    private LocalDateTime licenseEndDate;
    private LocalDateTime createdAt;
}
