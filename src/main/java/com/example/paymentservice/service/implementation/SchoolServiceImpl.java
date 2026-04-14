package com.example.paymentservice.service.implementation;

import com.example.paymentservice.dto.DashboardSummaryResponse;
import com.example.paymentservice.dto.PaymentAuditTrailResponse;
import com.example.paymentservice.dto.PlanResponse;
import com.example.paymentservice.dto.SchoolViewResponse;
import com.example.paymentservice.dto.TrendPointResponse;
import com.example.paymentservice.entity.InvoiceDocument;
import com.example.paymentservice.entity.PaymentAuditTrail;
import com.example.paymentservice.entity.Plan;
import com.example.paymentservice.entity.School;
import com.example.paymentservice.enums.InvoiceStatus;
import com.example.paymentservice.exception.PaymentException;
import com.example.paymentservice.repository.InvoiceDocumentRepository;
import com.example.paymentservice.repository.PaymentAuditTrailRepository;
import com.example.paymentservice.repository.PlanRepository;
import com.example.paymentservice.repository.SchoolRepository;
import com.example.paymentservice.service.SchoolService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SchoolServiceImpl implements SchoolService {

    private final SchoolRepository schoolRepository;
    private final PlanRepository planRepository;
    private final PaymentAuditTrailRepository paymentAuditTrailRepository;
    private final InvoiceDocumentRepository invoiceDocumentRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        List<PaymentAuditTrail> trails = paymentAuditTrailRepository.findAllByOrderByCreatedAtDesc();
        List<InvoiceDocument> invoices = invoiceDocumentRepository.findAll();
        BigDecimal totalRevenue = trails.stream()
                .filter(trail -> "SUCCESS".equalsIgnoreCase(trail.getPaymentStatus()))
                .map(PaymentAuditTrail::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal currentMonthRevenue = trails.stream()
                .filter(trail -> "SUCCESS".equalsIgnoreCase(trail.getPaymentStatus()))
                .filter(trail -> trail.getPaidAt() != null
                        && trail.getPaidAt().getMonth().equals(LocalDate.now().getMonth())
                        && trail.getPaidAt().getYear() == LocalDate.now().getYear())
                .map(PaymentAuditTrail::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return DashboardSummaryResponse.builder()
                .totalSchools(schoolRepository.count())
                .activeSchools(schoolRepository.countByStatus("ACTIVE"))
                .inactiveSchools(schoolRepository.countByStatus("INACTIVE"))
                .paidInvoices(invoices.stream().filter(invoice -> InvoiceStatus.PAID.equals(invoice.getStatus())).count())
                .totalRevenue(totalRevenue)
                .currentMonthRevenue(currentMonthRevenue)
                .monthlyTrend(buildMonthlyTrend(trails))
                .paymentStatusTrend(buildPaymentStatusTrend(trails))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SchoolViewResponse> getAllSchools() {
        return schoolRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::mapSchool)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SchoolViewResponse getSchool(String schoolCode) {
        return mapSchool(schoolRepository.findBySchoolCode(schoolCode)
                .orElseThrow(() -> new PaymentException("School not found: " + schoolCode)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentAuditTrailResponse> getSchoolPayments(String schoolCode) {
        return paymentAuditTrailRepository.findBySchoolCodeOrderByCreatedAtDesc(schoolCode)
                .stream()
                .map(this::mapTrail)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlanResponse> getAllPlans() {
        return planRepository.findByStatusOrderByPriceAsc("ACTIVE")
                .stream()
                .map(this::mapPlan)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentAuditTrailResponse> getAllPayments() {
        return paymentAuditTrailRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::mapTrail)
                .toList();
    }

    private List<TrendPointResponse> buildMonthlyTrend(List<PaymentAuditTrail> trails) {
        List<TrendPointResponse> trend = new ArrayList<>();
        for (int index = 5; index >= 0; index--) {
            YearMonth month = YearMonth.now().minusMonths(index);
            BigDecimal revenue = trails.stream()
                    .filter(trail -> "SUCCESS".equalsIgnoreCase(trail.getPaymentStatus()))
                    .filter(trail -> trail.getPaidAt() != null && YearMonth.from(trail.getPaidAt()).equals(month))
                    .map(PaymentAuditTrail::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long payments = trails.stream()
                    .filter(trail -> trail.getCreatedAt() != null && YearMonth.from(trail.getCreatedAt()).equals(month))
                    .count();
            trend.add(TrendPointResponse.builder()
                    .label(month.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                    .revenue(revenue)
                    .payments(payments)
                    .build());
        }
        return trend;
    }

    private List<TrendPointResponse> buildPaymentStatusTrend(List<PaymentAuditTrail> trails) {
        return List.of(
                TrendPointResponse.builder()
                        .label("SUCCESS")
                        .revenue(totalByStatus(trails, "SUCCESS"))
                        .payments(countByStatus(trails, "SUCCESS"))
                        .build(),
                TrendPointResponse.builder()
                        .label("FAILED")
                        .revenue(totalByStatus(trails, "FAILED"))
                        .payments(countByStatus(trails, "FAILED"))
                        .build(),
                TrendPointResponse.builder()
                        .label("CREATED")
                        .revenue(totalByStatus(trails, "CREATED"))
                        .payments(countByStatus(trails, "CREATED"))
                        .build()
        );
    }

    private BigDecimal totalByStatus(List<PaymentAuditTrail> trails, String status) {
        return trails.stream()
                .filter(trail -> status.equalsIgnoreCase(trail.getPaymentStatus()))
                .map(trail -> trail.getTotalAmount() == null ? BigDecimal.ZERO : trail.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long countByStatus(List<PaymentAuditTrail> trails, String status) {
        return trails.stream().filter(trail -> status.equalsIgnoreCase(trail.getPaymentStatus())).count();
    }

    private SchoolViewResponse mapSchool(School school) {
        Plan plan = school.getPlanId() == null ? null : planRepository.findById(school.getPlanId()).orElse(null);
        return SchoolViewResponse.builder()
                .id(school.getId())
                .name(school.getName())
                .schoolCode(school.getSchoolCode())
                .board(school.getBoard())
                .medium(school.getMedium())
                .email(school.getEmail())
                .phone(school.getPhone())
                .alternatePhone(school.getAlternatePhone())
                .addressLine1(school.getAddressLine1())
                .addressLine2(school.getAddressLine2())
                .city(school.getCity())
                .state(school.getState())
                .pincode(school.getPincode())
                .country(school.getCountry())
                .adminName(school.getAdminName())
                .adminEmail(school.getAdminEmail())
                .adminUsername(school.getAdminUsername())
                .schemaName(school.getSchemaName())
                .status(school.getStatus())
                .planId(school.getPlanId())
                .planName(plan == null ? null : plan.getName())
                .planDurationInMonths(plan == null ? null : plan.getDurationInMonths())
                .planStatus(plan == null ? null : plan.getStatus())
                .licenseStartDate(school.getLicenseStartDate())
                .licenseEndDate(school.getLicenseEndDate())
                .createdAt(school.getCreatedAt())
                .build();
    }

    private PlanResponse mapPlan(Plan plan) {
        return PlanResponse.builder()
                .id(plan.getId())
                .name(plan.getName())
                .durationInMonths(plan.getDurationInMonths())
                .price(plan.getPrice())
                .status(plan.getStatus())
                .build();
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
