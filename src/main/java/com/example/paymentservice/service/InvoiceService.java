package com.example.paymentservice.service;

import com.example.paymentservice.dto.InvoiceResponse;
import com.example.paymentservice.entity.PaymentAuditTrail;
import java.util.List;

public interface InvoiceService {

    InvoiceResponse createInvoiceForPayment(PaymentAuditTrail paymentAuditTrail);

    List<InvoiceResponse> getAllInvoices();

    List<InvoiceResponse> getInvoicesBySchoolCode(String schoolCode);

    InvoiceResponse getInvoice(String invoiceNumber);

    byte[] downloadInvoicePdf(String invoiceNumber);
}
