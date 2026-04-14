package com.example.paymentservice.service.implementation;

import com.example.paymentservice.dto.InvoiceResponse;
import com.example.paymentservice.entity.InvoiceDocument;
import com.example.paymentservice.entity.PaymentAuditTrail;
import com.example.paymentservice.entity.School;
import com.example.paymentservice.enums.InvoiceStatus;
import com.example.paymentservice.exception.PaymentException;
import com.example.paymentservice.repository.InvoiceDocumentRepository;
import com.example.paymentservice.repository.PaymentAuditTrailRepository;
import com.example.paymentservice.repository.SchoolRepository;
import com.example.paymentservice.service.InvoiceService;
import com.example.paymentservice.util.InvoicePdfGenerator;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceDocumentRepository invoiceDocumentRepository;
    private final SchoolRepository schoolRepository;
    private final PaymentAuditTrailRepository paymentAuditTrailRepository;
    private final InvoicePdfGenerator invoicePdfGenerator;

    @Override
    @Transactional
    public InvoiceResponse createInvoiceForPayment(PaymentAuditTrail paymentAuditTrail) {
        return invoiceDocumentRepository.findByOrderId(paymentAuditTrail.getOrderId())
                .map(this::mapToResponse)
                .orElseGet(() -> mapToResponse(createAndSaveInvoice(paymentAuditTrail)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getAllInvoices() {
        return invoiceDocumentRepository.findAllByOrderByInvoiceDateDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponse> getInvoicesBySchoolCode(String schoolCode) {
        return invoiceDocumentRepository.findBySchoolCodeOrderByInvoiceDateDesc(schoolCode)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getInvoice(String invoiceNumber) {
        return mapToResponse(getInvoiceDocument(invoiceNumber));
    }

    @Override
    @Transactional
    public byte[] downloadInvoicePdf(String invoiceNumber) {
        return getInvoiceDocument(invoiceNumber).getPdfData();
    }

    private InvoiceDocument createAndSaveInvoice(PaymentAuditTrail trail) {
        School school = schoolRepository.findBySchoolCode(trail.getSchoolCode())
                .orElseThrow(() -> new PaymentException("School not found for invoice creation: " + trail.getSchoolCode()));

        LocalDate invoiceDate = LocalDate.now();
        String invoiceNumber = trail.getInvoiceNumber() != null ? trail.getInvoiceNumber() : 
                "INV-" + school.getSchoolCode() + "-" + invoiceDate.toString().replace("-", "") + "-" + trail.getId();

        InvoiceDocument invoiceDocument = InvoiceDocument.builder()
                .invoiceNumber(invoiceNumber)
                .orderId(trail.getOrderId())
                .paymentId(trail.getPaymentId())
                .schoolCode(school.getSchoolCode())
                .schoolName(school.getName())
                .schoolGstin(null)
                .billingAddress(buildAddress(school))
                .planName(trail.getPlanName())
                .billingPeriodLabel((trail.getPlanDurationInMonths() == null ? 12 : trail.getPlanDurationInMonths()) + " month subscription")
                .invoiceDate(invoiceDate)
                .dueDate(invoiceDate)
                .taxableAmount(trail.getTaxableAmount())
                .cgstRate(trail.getTaxableAmount() == null ? java.math.BigDecimal.ZERO : java.math.BigDecimal.valueOf(9))
                .cgstAmount(trail.getCgstAmount())
                .sgstRate(trail.getTaxableAmount() == null ? java.math.BigDecimal.ZERO : java.math.BigDecimal.valueOf(9))
                .sgstAmount(trail.getSgstAmount())
                .totalAmount(trail.getTotalAmount())
                .amountPaid(trail.getTotalAmount())
                .currency(trail.getCurrency())
                .status(InvoiceStatus.PAID)
                .notes("Subscription invoice generated from payment audit trail.")
                .build();

        invoiceDocument.setPdfData(invoicePdfGenerator.generate(invoiceDocument));
        InvoiceDocument saved = invoiceDocumentRepository.save(invoiceDocument);
        log.info("Invoice generated orderId={} paymentId={} invoiceNumber={}",
                trail.getOrderId(), trail.getPaymentId(), invoiceNumber);
        return saved;
    }

    private InvoiceDocument getInvoiceDocument(String invoiceNumber) {
        return invoiceDocumentRepository.findByInvoiceNumber(invoiceNumber)
                .orElseGet(() -> {
                    log.info("Invoice document not found for number={}, attempting lazy generation from audit trail", invoiceNumber);
                    PaymentAuditTrail trail = paymentAuditTrailRepository.findByInvoiceNumber(invoiceNumber)
                            .orElseThrow(() -> new PaymentException("Invoice not found: " + invoiceNumber));
                    return createAndSaveInvoice(trail);
                });
    }

    private String buildAddress(School school) {
        return String.join(", ",
                safe(school.getAddressLine1()),
                safe(school.getAddressLine2()),
                safe(school.getCity()),
                safe(school.getState()),
                safe(school.getPincode()),
                safe(school.getCountry()));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private InvoiceResponse mapToResponse(InvoiceDocument invoiceDocument) {
        return InvoiceResponse.builder()
                .invoiceNumber(invoiceDocument.getInvoiceNumber())
                .orderId(invoiceDocument.getOrderId())
                .paymentId(invoiceDocument.getPaymentId())
                .schoolCode(invoiceDocument.getSchoolCode())
                .schoolName(invoiceDocument.getSchoolName())
                .planName(invoiceDocument.getPlanName())
                .billingPeriodLabel(invoiceDocument.getBillingPeriodLabel())
                .invoiceDate(invoiceDocument.getInvoiceDate())
                .dueDate(invoiceDocument.getDueDate())
                .taxableAmount(invoiceDocument.getTaxableAmount())
                .cgstAmount(invoiceDocument.getCgstAmount())
                .sgstAmount(invoiceDocument.getSgstAmount())
                .totalAmount(invoiceDocument.getTotalAmount())
                .currency(invoiceDocument.getCurrency())
                .status(invoiceDocument.getStatus())
                .downloadUrl("/api/invoices/" + invoiceDocument.getInvoiceNumber() + "/download")
                .build();
    }
}
