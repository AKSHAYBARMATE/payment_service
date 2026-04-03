package com.example.paymentservice.service.implementation;

import com.example.paymentservice.config.RazorpayProperties;
import com.example.paymentservice.dto.CreateOrderRequest;
import com.example.paymentservice.dto.CreateOrderResponse;
import com.example.paymentservice.dto.PaymentTransactionResponse;
import com.example.paymentservice.dto.VerifyPaymentRequest;
import com.example.paymentservice.entity.PaymentTransaction;
import com.example.paymentservice.enums.PaymentStatus;
import com.example.paymentservice.exception.PaymentException;
import com.example.paymentservice.exception.RazorpayIntegrationException;
import com.example.paymentservice.repository.PaymentTransactionRepository;
import com.example.paymentservice.service.PaymentService;
import com.example.paymentservice.util.RazorpaySignatureUtil;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final RazorpayClient razorpayClient;
    private final RazorpayProperties razorpayProperties;
    private final PaymentTransactionRepository paymentTransactionRepository;
    private final RazorpaySignatureUtil razorpaySignatureUtil;

    @Override
    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        log.info("Payment creation request received userId={} orderId={} paymentId={} amount={} currency={}",
                request.getUserId(), "NA", "NA", request.getAmount(), request.getCurrency());

        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", convertToSubunits(request.getAmount()));
            orderRequest.put("currency", request.getCurrency().toUpperCase());
            orderRequest.put("receipt", buildReceipt(request.getUserId()));
            orderRequest.put("payment_capture", 1);

            Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String orderId = razorpayOrder.get("id");

            log.info("Razorpay API response received userId={} orderId={} paymentId={} response={}",
                    request.getUserId(), orderId, "NA", razorpayOrder);

            PaymentTransaction paymentTransaction = PaymentTransaction.builder()
                    .orderId(orderId)
                    .userId(request.getUserId())
                    .amount(request.getAmount().setScale(2, RoundingMode.HALF_UP))
                    .currency(request.getCurrency().toUpperCase())
                    .paymentStatus(PaymentStatus.CREATED)
                    .description(request.getDescription())
                    .metadata(request.getMetadata())
                    .build();

            paymentTransactionRepository.save(paymentTransaction);

            log.info("Payment transaction persisted userId={} orderId={} paymentId={} status={}",
                    request.getUserId(), orderId, "NA", paymentTransaction.getPaymentStatus());

            return CreateOrderResponse.builder()
                    .orderId(orderId)
                    .razorpayKey(razorpayProperties.getKey())
                    .userId(paymentTransaction.getUserId())
                    .amount(paymentTransaction.getAmount())
                    .currency(paymentTransaction.getCurrency())
                    .paymentStatus(paymentTransaction.getPaymentStatus())
                    .description(paymentTransaction.getDescription())
                    .build();
        } catch (RazorpayException exception) {
            log.error("Razorpay order creation failed userId={} orderId={} paymentId={} error={}",
                    request.getUserId(), "NA", "NA", exception.getMessage(), exception);
            throw new RazorpayIntegrationException("Failed to create order in Razorpay", exception);
        }
    }

    @Override
    @Transactional
    public PaymentTransactionResponse verifyPayment(VerifyPaymentRequest request) {
        log.info("Payment verification requested userId={} orderId={} paymentId={}",
                "UNKNOWN", request.getOrderId(), request.getPaymentId());

        PaymentTransaction paymentTransaction = paymentTransactionRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new PaymentException("Payment transaction not found for orderId: " + request.getOrderId()));

        String userId = paymentTransaction.getUserId();
        log.info("Loaded payment transaction for verification userId={} orderId={} paymentId={} status={}",
                userId, request.getOrderId(), request.getPaymentId(), paymentTransaction.getPaymentStatus());

        if (PaymentStatus.SUCCESS.equals(paymentTransaction.getPaymentStatus())) {
            if (request.getPaymentId().equals(paymentTransaction.getPaymentId())) {
                log.info("Idempotent verification request accepted userId={} orderId={} paymentId={}",
                        userId, request.getOrderId(), request.getPaymentId());
                return mapToResponse(paymentTransaction);
            }
            throw new PaymentException("Order is already verified with a different paymentId");
        }

        paymentTransactionRepository.findByPaymentId(request.getPaymentId())
                .filter(existingTransaction -> !existingTransaction.getOrderId().equals(request.getOrderId()))
                .ifPresent(existingTransaction -> {
                    throw new PaymentException("Payment ID is already associated with another order");
                });

        boolean validSignature = razorpaySignatureUtil.verifySignature(
                request.getOrderId(),
                request.getPaymentId(),
                request.getRazorpaySignature()
        );

        log.info("Payment verification result userId={} orderId={} paymentId={} isValid={}",
                userId, request.getOrderId(), request.getPaymentId(), validSignature);

        if (!validSignature) {
            paymentTransaction.setPaymentStatus(PaymentStatus.FAILED);
            paymentTransaction.setPaymentId(request.getPaymentId());
            paymentTransaction.setRazorpaySignature(request.getRazorpaySignature());
            paymentTransaction.setPaymentMethod(request.getPaymentMethod());
            paymentTransactionRepository.save(paymentTransaction);

            log.warn("Invalid payment signature stored userId={} orderId={} paymentId={} status={}",
                    userId, request.getOrderId(), request.getPaymentId(), paymentTransaction.getPaymentStatus());
            throw new PaymentException("Invalid Razorpay signature");
        }

        paymentTransaction.setPaymentId(request.getPaymentId());
        paymentTransaction.setRazorpaySignature(request.getRazorpaySignature());
        paymentTransaction.setPaymentMethod(request.getPaymentMethod());
        paymentTransaction.setPaymentStatus(PaymentStatus.SUCCESS);
        paymentTransactionRepository.save(paymentTransaction);

        log.info("Payment transaction updated successfully userId={} orderId={} paymentId={} status={}",
                userId, request.getOrderId(), request.getPaymentId(), paymentTransaction.getPaymentStatus());

        return mapToResponse(paymentTransaction);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentTransactionResponse> getPaymentHistoryByUserId(String userId) {
        log.info("Fetching payment history by user userId={} orderId={} paymentId={}", userId, "NA", "NA");
        return paymentTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentTransactionResponse getPaymentByOrderId(String orderId) {
        PaymentTransaction paymentTransaction = paymentTransactionRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentException("Payment transaction not found for orderId: " + orderId));
        log.info("Fetched payment by orderId userId={} orderId={} paymentId={}",
                paymentTransaction.getUserId(), orderId, paymentTransaction.getPaymentId());
        return mapToResponse(paymentTransaction);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentTransactionResponse> getAllPayments() {
        log.info("Fetching complete payment history userId={} orderId={} paymentId={}", "ALL", "NA", "NA");
        return paymentTransactionRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private long convertToSubunits(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private String buildReceipt(String userId) {
        String sanitizedUserId = userId.replaceAll("[^a-zA-Z0-9]", "");
        String suffix = String.valueOf(System.currentTimeMillis());
        return "rcpt_" + sanitizedUserId + "_" + suffix.substring(Math.max(0, suffix.length() - 8));
    }

    private PaymentTransactionResponse mapToResponse(PaymentTransaction paymentTransaction) {
        return PaymentTransactionResponse.builder()
                .id(paymentTransaction.getId())
                .orderId(paymentTransaction.getOrderId())
                .paymentId(paymentTransaction.getPaymentId())
                .userId(paymentTransaction.getUserId())
                .amount(paymentTransaction.getAmount())
                .currency(paymentTransaction.getCurrency())
                .paymentStatus(paymentTransaction.getPaymentStatus())
                .paymentMethod(paymentTransaction.getPaymentMethod())
                .description(paymentTransaction.getDescription())
                .metadata(paymentTransaction.getMetadata())
                .createdAt(paymentTransaction.getCreatedAt())
                .updatedAt(paymentTransaction.getUpdatedAt())
                .build();
    }
}
