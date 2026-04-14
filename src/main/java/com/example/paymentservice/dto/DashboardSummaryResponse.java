package com.example.paymentservice.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {

    private long totalSchools;
    private long activeSchools;
    private long inactiveSchools;
    private long paidInvoices;
    private BigDecimal totalRevenue;
    private BigDecimal currentMonthRevenue;
    private List<TrendPointResponse> monthlyTrend;
    private List<TrendPointResponse> paymentStatusTrend;
}
