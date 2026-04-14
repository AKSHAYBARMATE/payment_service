package com.example.paymentservice.controller;

import com.example.paymentservice.dto.ApiResponse;
import com.example.paymentservice.dto.DashboardSummaryResponse;
import com.example.paymentservice.dto.PaymentAuditTrailResponse;
import com.example.paymentservice.dto.PlanResponse;
import com.example.paymentservice.dto.SchoolViewResponse;
import com.example.paymentservice.service.SchoolService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/schools")
@RequiredArgsConstructor
@Tag(name = "School ERP APIs", description = "Read school details, dashboard insights, plans, and payment audit trail")
public class SchoolController {

    private final SchoolService schoolService;

    @GetMapping("/summary")
    @Operation(summary = "Get dashboard summary", description = "Returns high-level counts, revenue metrics, and monthly payment trends.")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary() {
        return ResponseEntity.ok(ApiResponse.success("Dashboard summary fetched successfully", schoolService.getSummary()));
    }

    @GetMapping
    @Operation(summary = "Get all schools", description = "Returns all schools with license and plan information.")
    public ResponseEntity<ApiResponse<List<SchoolViewResponse>>> getAllSchools() {
        return ResponseEntity.ok(ApiResponse.success("Schools fetched successfully", schoolService.getAllSchools()));
    }

    @GetMapping("/{schoolCode}")
    @Operation(summary = "Get school details", description = "Returns one school detail record using schoolCode.")
    public ResponseEntity<ApiResponse<SchoolViewResponse>> getSchool(@PathVariable String schoolCode) {
        return ResponseEntity.ok(ApiResponse.success("School fetched successfully", schoolService.getSchool(schoolCode)));
    }

    @GetMapping("/{schoolCode}/payments")
    @Operation(summary = "Get school payment audit trail", description = "Returns payment audit trail rows for a specific school.")
    public ResponseEntity<ApiResponse<List<PaymentAuditTrailResponse>>> getSchoolPayments(@PathVariable String schoolCode) {
        return ResponseEntity.ok(ApiResponse.success("School payments fetched successfully", schoolService.getSchoolPayments(schoolCode)));
    }

    @GetMapping("/plans/all")
    @Operation(summary = "Get active plans", description = "Returns active plans for display in the dashboard.")
    public ResponseEntity<ApiResponse<List<PlanResponse>>> getPlans() {
        return ResponseEntity.ok(ApiResponse.success("Plans fetched successfully", schoolService.getAllPlans()));
    }

    @GetMapping("/payments/all")
    @Operation(summary = "Get all payment audit records", description = "Returns the global payment audit trail across all schools.")
    public ResponseEntity<ApiResponse<List<PaymentAuditTrailResponse>>> getAllPayments() {
        return ResponseEntity.ok(ApiResponse.success("Payment audit trail fetched successfully", schoolService.getAllPayments()));
    }
}
