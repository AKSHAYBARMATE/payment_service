package com.example.paymentservice.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@Entity
@Table(name = "schools")
public class School {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    private String adminPassword;

    private String schemaName;
    private String status = "ACTIVE";

    private Long planId;
    private LocalDateTime licenseStartDate;
    private LocalDateTime licenseEndDate;

    private LocalDateTime createdAt = LocalDateTime.now();
}
