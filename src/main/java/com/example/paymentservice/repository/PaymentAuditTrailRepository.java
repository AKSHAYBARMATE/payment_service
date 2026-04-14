package com.example.paymentservice.repository;

import com.example.paymentservice.entity.PaymentAuditTrail;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentAuditTrailRepository extends JpaRepository<PaymentAuditTrail, Long> {

    Optional<PaymentAuditTrail> findByOrderId(String orderId);

    Optional<PaymentAuditTrail> findByPaymentId(String paymentId);

    Optional<PaymentAuditTrail> findByInvoiceNumber(String invoiceNumber);

    List<PaymentAuditTrail> findByInitiatedByUserIdOrderByCreatedAtDesc(String initiatedByUserId);

    List<PaymentAuditTrail> findBySchoolCodeOrderByCreatedAtDesc(String schoolCode);

    List<PaymentAuditTrail> findAllByOrderByCreatedAtDesc();

    List<PaymentAuditTrail> findByCreatedAtBetweenOrderByCreatedAtAsc(LocalDateTime startDate, LocalDateTime endDate);
}
