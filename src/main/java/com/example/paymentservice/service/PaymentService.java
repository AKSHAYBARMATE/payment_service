package com.example.paymentservice.service;

import com.example.paymentservice.dto.CreateOrderRequest;
import com.example.paymentservice.dto.CreateOrderResponse;
import com.example.paymentservice.dto.PaymentTransactionResponse;
import com.example.paymentservice.dto.VerifyPaymentRequest;
import java.util.List;

public interface PaymentService {

    CreateOrderResponse createOrder(CreateOrderRequest request);

    PaymentTransactionResponse verifyPayment(VerifyPaymentRequest request);

    List<PaymentTransactionResponse> getPaymentHistoryByUserId(String userId);

    PaymentTransactionResponse getPaymentByOrderId(String orderId);

    List<PaymentTransactionResponse> getAllPayments();
}
