package com.example.paymentservice.config;

import com.example.paymentservice.entity.PaymentAuditTrail;
import com.example.paymentservice.entity.Plan;
import com.example.paymentservice.entity.School;
import com.example.paymentservice.repository.PaymentAuditTrailRepository;
import com.example.paymentservice.repository.PlanRepository;
import com.example.paymentservice.repository.SchoolRepository;
import com.example.paymentservice.service.InvoiceService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final SchoolRepository schoolRepository;
    private final PlanRepository planRepository;
    private final PaymentAuditTrailRepository paymentAuditTrailRepository;
    private final InvoiceService invoiceService;

    @Bean
    public CommandLineRunner seedMasterData() {
        return args -> {
            if (planRepository.count() == 0) {
                Plan starter = new Plan();
                starter.setName("Starter");
                starter.setDurationInMonths(6);
                starter.setPrice(BigDecimal.valueOf(12000));
                planRepository.save(starter);

                Plan growth = new Plan();
                growth.setName("Growth");
                growth.setDurationInMonths(12);
                growth.setPrice(BigDecimal.valueOf(36000));
                planRepository.save(growth);

                Plan enterprise = new Plan();
                enterprise.setName("Enterprise");
                enterprise.setDurationInMonths(12);
                enterprise.setPrice(BigDecimal.valueOf(75000));
                planRepository.save(enterprise);
            }

            if (schoolRepository.count() == 0) {
                Long starterPlanId = planRepository.findByName("Starter").map(Plan::getId).orElse(null);
                Long growthPlanId = planRepository.findByName("Growth").map(Plan::getId).orElse(null);
                Long enterprisePlanId = planRepository.findByName("Enterprise").map(Plan::getId).orElse(null);

                schoolRepository.save(buildSchool(
                        "Green Valley Public School", "SCH-DEL-001", "CBSE", "English",
                        "support@greenvalley.edu.in", "9810001111", "9810002222",
                        "12 Ring Road", "Pitampura", "Delhi", "Delhi", "110034", "India",
                        "Aarav Mehta", "admin@greenvalley.edu.in", "greenvalley_admin", "secret",
                        "school_green_valley", "ACTIVE", growthPlanId,
                        LocalDateTime.now().minusMonths(2), LocalDateTime.now().plusMonths(10)
                ));

                schoolRepository.save(buildSchool(
                        "Sunrise International Academy", "SCH-PUN-002", "ICSE", "English",
                        "accounts@sunriseacademy.edu.in", "9822004545", "9822001111",
                        "45 Prabhat Road", "Erandwane", "Pune", "Maharashtra", "411004", "India",
                        "Neha Kulkarni", "principal@sunriseacademy.edu.in", "sunrise_admin", "secret",
                        "school_sunrise", "ACTIVE", starterPlanId,
                        LocalDateTime.now().minusMonths(1), LocalDateTime.now().plusMonths(5)
                ));

                schoolRepository.save(buildSchool(
                        "Lotus World School", "SCH-BLR-003", "State Board", "English",
                        "principal@lotusworld.edu.in", "9845007766", "9845008899",
                        "88 Outer Ring Road", "Marathahalli", "Bengaluru", "Karnataka", "560037", "India",
                        "Ritika Nair", "admin@lotusworld.edu.in", "lotus_admin", "secret",
                        "school_lotus", "INACTIVE", enterprisePlanId,
                        LocalDateTime.now().minusMonths(14), LocalDateTime.now().minusDays(10)
                ));
            }

            if (paymentAuditTrailRepository.count() == 0) {
                School schoolOne = schoolRepository.findBySchoolCode("SCH-DEL-001").orElse(null);
                School schoolTwo = schoolRepository.findBySchoolCode("SCH-PUN-002").orElse(null);
                School schoolThree = schoolRepository.findBySchoolCode("SCH-BLR-003").orElse(null);
                Plan growth = planRepository.findByName("Growth").orElse(null);
                Plan starter = planRepository.findByName("Starter").orElse(null);
                Plan enterprise = planRepository.findByName("Enterprise").orElse(null);

                PaymentAuditTrail trailOne = saveTrail(
                        schoolOne, growth, "erp-superadmin", "order_demo_001", "pay_demo_001",
                        BigDecimal.valueOf(36000), "SUCCESS", "online", "PAYMENT_VERIFIED",
                        LocalDateTime.now().minusMonths(2)
                );
                trailOne.setInvoiceNumber(invoiceService.createInvoiceForPayment(trailOne).getInvoiceNumber());
                paymentAuditTrailRepository.save(trailOne);

                PaymentAuditTrail trailTwo = saveTrail(
                        schoolTwo, starter, "erp-superadmin", "order_demo_002", "pay_demo_002",
                        BigDecimal.valueOf(12000), "SUCCESS", "upi", "PAYMENT_VERIFIED",
                        LocalDateTime.now().minusDays(18)
                );
                trailTwo.setInvoiceNumber(invoiceService.createInvoiceForPayment(trailTwo).getInvoiceNumber());
                paymentAuditTrailRepository.save(trailTwo);

                saveTrail(
                        schoolThree, enterprise, "erp-superadmin", "order_demo_003", "pay_demo_003",
                        BigDecimal.valueOf(75000), "FAILED", "card", "PAYMENT_VERIFICATION_FAILED",
                        null
                );
            }
        };
    }

    private School buildSchool(
            String name,
            String schoolCode,
            String board,
            String medium,
            String email,
            String phone,
            String alternatePhone,
            String addressLine1,
            String addressLine2,
            String city,
            String state,
            String pincode,
            String country,
            String adminName,
            String adminEmail,
            String adminUsername,
            String adminPassword,
            String schemaName,
            String status,
            Long planId,
            LocalDateTime licenseStartDate,
            LocalDateTime licenseEndDate
    ) {
        School school = new School();
        school.setName(name);
        school.setSchoolCode(schoolCode);
        school.setBoard(board);
        school.setMedium(medium);
        school.setEmail(email);
        school.setPhone(phone);
        school.setAlternatePhone(alternatePhone);
        school.setAddressLine1(addressLine1);
        school.setAddressLine2(addressLine2);
        school.setCity(city);
        school.setState(state);
        school.setPincode(pincode);
        school.setCountry(country);
        school.setAdminName(adminName);
        school.setAdminEmail(adminEmail);
        school.setAdminUsername(adminUsername);
        school.setAdminPassword(adminPassword);
        school.setSchemaName(schemaName);
        school.setStatus(status);
        school.setPlanId(planId);
        school.setLicenseStartDate(licenseStartDate);
        school.setLicenseEndDate(licenseEndDate);
        return school;
    }

    private PaymentAuditTrail saveTrail(
            School school,
            Plan plan,
            String userId,
            String orderId,
            String paymentId,
            BigDecimal totalAmount,
            String paymentStatus,
            String paymentMethod,
            String auditAction,
            LocalDateTime paidAt
    ) {
        PaymentAuditTrail trail = new PaymentAuditTrail();
        trail.setSchoolId(school == null ? null : school.getId());
        trail.setSchoolCode(school == null ? null : school.getSchoolCode());
        trail.setSchoolName(school == null ? null : school.getName());
        trail.setPlanId(plan == null ? null : plan.getId());
        trail.setPlanName(plan == null ? null : plan.getName());
        trail.setPlanDurationInMonths(plan == null ? null : plan.getDurationInMonths());
        trail.setInitiatedByUserId(userId);
        trail.setOrderId(orderId);
        trail.setPaymentId(paymentId);
        trail.setAmount(totalAmount);
        trail.setTaxableAmount(totalAmount.divide(BigDecimal.valueOf(1.18), 2, java.math.RoundingMode.HALF_UP));
        BigDecimal totalTax = totalAmount.subtract(trail.getTaxableAmount());
        trail.setCgstAmount(totalTax.divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.HALF_UP));
        trail.setSgstAmount(totalTax.subtract(trail.getCgstAmount()));
        trail.setTotalAmount(totalAmount);
        trail.setCurrency("INR");
        trail.setPaymentStatus(paymentStatus);
        trail.setPaymentMethod(paymentMethod);
        trail.setAuditAction(auditAction);
        trail.setDescription((plan == null ? "ERP" : plan.getName()) + " subscription");
        trail.setMetadata("{\"seedData\":true}");
        trail.setPaidAt(paidAt);
        if (paidAt != null && plan != null) {
            trail.setSubscriptionStartDate(paidAt);
            trail.setSubscriptionEndDate(paidAt.plusMonths(plan.getDurationInMonths()));
        }
        return paymentAuditTrailRepository.save(trail);
    }
}
