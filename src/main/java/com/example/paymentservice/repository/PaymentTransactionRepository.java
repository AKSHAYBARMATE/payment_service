package com.example.paymentservice.repository;

import com.example.paymentservice.entity.PaymentTransaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByOrderId(String orderId);

    Optional<PaymentTransaction> findByPaymentId(String paymentId);

    List<PaymentTransaction> findByUserIdOrderByCreatedAtDesc(String userId);

    List<PaymentTransaction> findAllByOrderByCreatedAtDesc();
}
