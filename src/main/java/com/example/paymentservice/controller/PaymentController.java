package com.example.paymentservice.controller;

import com.example.paymentservice.dto.ApiResponse;
import com.example.paymentservice.dto.CreateOrderRequest;
import com.example.paymentservice.dto.CreateOrderResponse;
import com.example.paymentservice.dto.PaymentAuditTrailResponse;
import com.example.paymentservice.dto.VerifyPaymentRequest;
import com.example.paymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
@Tag(name = "Payment APIs", description = "Endpoints for Razorpay order creation, payment verification, and history retrieval")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create-order")
    @Operation(summary = "Create Razorpay order", description = "Creates a Razorpay order and stores the transaction in MySQL with CREATED status.")
    public ResponseEntity<ApiResponse<CreateOrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        CreateOrderResponse response = paymentService.createOrder(request);
        return ResponseEntity.ok(ApiResponse.success("Order created successfully", response));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify Razorpay payment", description = "Verifies Razorpay signature, updates payment status, and stores payment details idempotently.")
    public ResponseEntity<ApiResponse<PaymentAuditTrailResponse>> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request
    ) {
        PaymentAuditTrailResponse response = paymentService.verifyPayment(request);
        return ResponseEntity.ok(ApiResponse.success("Payment verified successfully", response));
    }

    @GetMapping("/history/{userId}")
    @Operation(summary = "Get payment history by user", description = "Fetches all payment transactions for a given user in reverse chronological order.")
    public ResponseEntity<ApiResponse<List<PaymentAuditTrailResponse>>> getPaymentHistoryByUserId(
            @PathVariable String userId
    ) {
        List<PaymentAuditTrailResponse> response = paymentService.getPaymentHistoryByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("Payment history fetched successfully", response));
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get payment by order ID", description = "Fetches a single payment transaction using the Razorpay order ID.")
    public ResponseEntity<ApiResponse<PaymentAuditTrailResponse>> getPaymentByOrderId(
            @PathVariable String orderId
    ) {
        PaymentAuditTrailResponse response = paymentService.getPaymentByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success("Payment fetched successfully", response));
    }

    @GetMapping("/history")
    @Operation(summary = "Get all payments", description = "Fetches the complete payment transaction history.")
    public ResponseEntity<ApiResponse<List<PaymentAuditTrailResponse>>> getAllPayments() {
        List<PaymentAuditTrailResponse> response = paymentService.getAllPayments();
        return ResponseEntity.ok(ApiResponse.success("All payments fetched successfully", response));
    }
}
