package com.example.paymentservice.service.implementation;

import com.example.paymentservice.config.RazorpayProperties;
import com.example.paymentservice.dto.CreateOrderRequest;
import com.example.paymentservice.dto.CreateOrderResponse;
import com.example.paymentservice.dto.PaymentAuditTrailResponse;
import com.example.paymentservice.dto.VerifyPaymentRequest;
import com.example.paymentservice.entity.PaymentAuditTrail;
import com.example.paymentservice.entity.Plan;
import com.example.paymentservice.entity.School;
import com.example.paymentservice.exception.PaymentException;
import com.example.paymentservice.exception.RazorpayIntegrationException;
import com.example.paymentservice.repository.PaymentAuditTrailRepository;
import com.example.paymentservice.repository.PlanRepository;
import com.example.paymentservice.repository.SchoolRepository;
import com.example.paymentservice.service.InvoiceService;
import com.example.paymentservice.service.PaymentService;
import com.example.paymentservice.util.RazorpaySignatureUtil;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
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
    private final PaymentAuditTrailRepository paymentAuditTrailRepository;
    private final SchoolRepository schoolRepository;
    private final PlanRepository planRepository;
    private final InvoiceService invoiceService;
    private final RazorpaySignatureUtil razorpaySignatureUtil;

    @Override
    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        School school = schoolRepository.findBySchoolCode(request.getSchoolCode())
                .orElseThrow(() -> new PaymentException("School not found for schoolCode: " + request.getSchoolCode()));
        Plan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new PaymentException("Plan not found for planId: " + request.getPlanId()));

        log.info("Payment creation request received userId={} orderId={} paymentId={} schoolCode={} planId={}",
                request.getUserId(), "NA", "NA", request.getSchoolCode(), request.getPlanId());

        try {
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", convertToSubunits(request.getAmount()));
            orderRequest.put("currency", request.getCurrency().toUpperCase());
            orderRequest.put("receipt", buildReceipt(request.getUserId(), request.getSchoolCode()));
            orderRequest.put("payment_capture", 1);

            Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            String orderId = razorpayOrder.get("id");

            PaymentAuditTrail trail = new PaymentAuditTrail();
            trail.setSchoolId(school.getId());
            trail.setSchoolCode(school.getSchoolCode());
            trail.setSchoolName(school.getName());
            trail.setPlanId(plan.getId());
            trail.setPlanName(plan.getName());
            trail.setPlanDurationInMonths(plan.getDurationInMonths());
            trail.setInitiatedByUserId(request.getUserId());
            trail.setOrderId(orderId);
            trail.setAmount(request.getAmount().setScale(2, RoundingMode.HALF_UP));
            trail.setTaxableAmount(BigDecimal.ZERO);
            trail.setCgstAmount(BigDecimal.ZERO);
            trail.setSgstAmount(BigDecimal.ZERO);
            trail.setTotalAmount(request.getAmount().setScale(2, RoundingMode.HALF_UP));
            trail.setCurrency(request.getCurrency().toUpperCase());
            trail.setPaymentStatus("CREATED");
            trail.setAuditAction("ORDER_CREATED");
            trail.setDescription(request.getDescription());
            trail.setMetadata(request.getMetadata());
            paymentAuditTrailRepository.save(trail);

            return CreateOrderResponse.builder()
                    .orderId(orderId)
                    .razorpayKey(razorpayProperties.getKey())
                    .userId(request.getUserId())
                    .schoolCode(school.getSchoolCode())
                    .schoolName(school.getName())
                    .planId(plan.getId())
                    .planName(plan.getName())
                    .amount(trail.getAmount())
                    .currency(trail.getCurrency())
                    .paymentStatus(trail.getPaymentStatus())
                    .description(request.getDescription())
                    .build();
        } catch (RazorpayException exception) {
            throw new RazorpayIntegrationException("Failed to create order in Razorpay", exception);
        }
    }

    @Override
    @Transactional
    public PaymentAuditTrailResponse verifyPayment(VerifyPaymentRequest request) {
        PaymentAuditTrail trail = paymentAuditTrailRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new PaymentException("Payment audit record not found for orderId: " + request.getOrderId()));

        if ("SUCCESS".equalsIgnoreCase(trail.getPaymentStatus())) {
            if (request.getPaymentId().equals(trail.getPaymentId())) {
                return mapTrail(trail);
            }
            throw new PaymentException("Order is already verified with a different paymentId");
        }

        paymentAuditTrailRepository.findByPaymentId(request.getPaymentId())
                .filter(existing -> !existing.getOrderId().equals(request.getOrderId()))
                .ifPresent(existing -> {
                    throw new PaymentException("Payment ID is already associated with another order");
                });

        boolean validSignature = razorpaySignatureUtil.verifySignature(
                request.getOrderId(),
                request.getPaymentId(),
                request.getRazorpaySignature()
        );

        trail.setPaymentId(request.getPaymentId());
        trail.setRazorpaySignature(request.getRazorpaySignature());
        trail.setPaymentMethod(request.getPaymentMethod());
        trail.setPaidAt(LocalDateTime.now());

        BigDecimal taxableAmount = trail.getAmount().divide(BigDecimal.valueOf(1.18), 2, RoundingMode.HALF_UP);
        BigDecimal taxAmount = trail.getAmount().subtract(taxableAmount).setScale(2, RoundingMode.HALF_UP);
        BigDecimal cgstAmount = taxAmount.divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP);
        BigDecimal sgstAmount = taxAmount.subtract(cgstAmount).setScale(2, RoundingMode.HALF_UP);
        trail.setTaxableAmount(taxableAmount);
        trail.setCgstAmount(cgstAmount);
        trail.setSgstAmount(sgstAmount);
        trail.setTotalAmount(trail.getAmount().setScale(2, RoundingMode.HALF_UP));

        if (!validSignature) {
            trail.setPaymentStatus("FAILED");
            trail.setAuditAction("PAYMENT_VERIFICATION_FAILED");
            paymentAuditTrailRepository.save(trail);
            throw new PaymentException("Invalid Razorpay signature");
        }

        trail.setPaymentStatus("SUCCESS");
        trail.setAuditAction("PAYMENT_VERIFIED");
        paymentAuditTrailRepository.save(trail);

        School school = schoolRepository.findBySchoolCode(trail.getSchoolCode())
                .orElseThrow(() -> new PaymentException("School not found for schoolCode: " + trail.getSchoolCode()));
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = startDate.plusMonths(trail.getPlanDurationInMonths() == null ? 12 : trail.getPlanDurationInMonths());

        school.setPlanId(trail.getPlanId());
        school.setStatus("ACTIVE");
        school.setLicenseStartDate(startDate);
        school.setLicenseEndDate(endDate);
        schoolRepository.save(school);

        trail.setSubscriptionStartDate(startDate);
        trail.setSubscriptionEndDate(endDate);
        trail.setInvoiceNumber(invoiceService.createInvoiceForPayment(trail).getInvoiceNumber());
        paymentAuditTrailRepository.save(trail);
        return mapTrail(trail);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentAuditTrailResponse> getPaymentHistoryByUserId(String userId) {
        return paymentAuditTrailRepository.findByInitiatedByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapTrail)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentAuditTrailResponse getPaymentByOrderId(String orderId) {
        return mapTrail(paymentAuditTrailRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentException("Payment audit record not found for orderId: " + orderId)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentAuditTrailResponse> getAllPayments() {
        return paymentAuditTrailRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapTrail)
                .toList();
    }

    private long convertToSubunits(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
    }

    private String buildReceipt(String userId, String schoolCode) {
        String suffix = String.valueOf(System.currentTimeMillis());
        return "rcpt_" + userId.replaceAll("[^a-zA-Z0-9]", "") + "_" + schoolCode + "_" + suffix.substring(Math.max(0, suffix.length() - 8));
    }

    private PaymentAuditTrailResponse mapTrail(PaymentAuditTrail trail) {
        LocalDateTime startDate = trail.getSubscriptionStartDate();
        if (startDate == null) {
            startDate = trail.getPaidAt() != null ? trail.getPaidAt() : trail.getCreatedAt();
        }

        LocalDateTime endDate = trail.getSubscriptionEndDate();
        if (endDate == null && startDate != null && trail.getPlanDurationInMonths() != null) {
            endDate = startDate.plusMonths(trail.getPlanDurationInMonths());
        }

        return PaymentAuditTrailResponse.builder()
                .id(trail.getId())
                .schoolId(trail.getSchoolId())
                .schoolCode(trail.getSchoolCode())
                .schoolName(trail.getSchoolName())
                .planId(trail.getPlanId())
                .planName(trail.getPlanName())
                .planDurationInMonths(trail.getPlanDurationInMonths())
                .initiatedByUserId(trail.getInitiatedByUserId())
                .orderId(trail.getOrderId())
                .paymentId(trail.getPaymentId())
                .amount(trail.getAmount())
                .taxableAmount(trail.getTaxableAmount())
                .cgstAmount(trail.getCgstAmount())
                .sgstAmount(trail.getSgstAmount())
                .totalAmount(trail.getTotalAmount())
                .currency(trail.getCurrency())
                .paymentStatus(trail.getPaymentStatus())
                .paymentMethod(trail.getPaymentMethod())
                .auditAction(trail.getAuditAction())
                .invoiceNumber(trail.getInvoiceNumber())
                .downloadUrl(trail.getInvoiceNumber() == null ? null : "/api/invoices/" + trail.getInvoiceNumber() + "/download")
                .subscriptionStartDate(startDate)
                .subscriptionEndDate(endDate)
                .description(trail.getDescription())
                .metadata(trail.getMetadata())
                .paidAt(trail.getPaidAt())
                .createdAt(trail.getCreatedAt())
                .updatedAt(trail.getUpdatedAt())
                .build();
    }
}
