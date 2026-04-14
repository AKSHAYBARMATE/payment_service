package com.example.paymentservice.repository;

import com.example.paymentservice.entity.Plan;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, Long> {

    Optional<Plan> findByName(String name);

    List<Plan> findByStatusOrderByPriceAsc(String status);
}
