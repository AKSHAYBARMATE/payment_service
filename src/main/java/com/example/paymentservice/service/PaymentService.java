package com.example.paymentservice.service;

import com.example.paymentservice.dto.CreateOrderRequest;
import com.example.paymentservice.dto.CreateOrderResponse;
import com.example.paymentservice.dto.PaymentAuditTrailResponse;
import com.example.paymentservice.dto.VerifyPaymentRequest;
import java.util.List;

public interface PaymentService {

    CreateOrderResponse createOrder(CreateOrderRequest request);

    PaymentAuditTrailResponse verifyPayment(VerifyPaymentRequest request);

    List<PaymentAuditTrailResponse> getPaymentHistoryByUserId(String userId);

    PaymentAuditTrailResponse getPaymentByOrderId(String orderId);

    List<PaymentAuditTrailResponse> getAllPayments();
}
