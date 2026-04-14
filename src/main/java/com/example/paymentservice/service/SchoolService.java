package com.example.paymentservice.service;

import com.example.paymentservice.dto.DashboardSummaryResponse;
import com.example.paymentservice.dto.PaymentAuditTrailResponse;
import com.example.paymentservice.dto.PlanResponse;
import com.example.paymentservice.dto.SchoolViewResponse;
import java.util.List;

public interface SchoolService {

    DashboardSummaryResponse getSummary();

    List<SchoolViewResponse> getAllSchools();

    SchoolViewResponse getSchool(String schoolCode);

    List<PaymentAuditTrailResponse> getSchoolPayments(String schoolCode);

    List<PlanResponse> getAllPlans();

    List<PaymentAuditTrailResponse> getAllPayments();
}
