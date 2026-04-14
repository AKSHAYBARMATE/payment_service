package com.example.paymentservice.repository;

import com.example.paymentservice.entity.InvoiceDocument;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceDocumentRepository extends JpaRepository<InvoiceDocument, Long> {

    Optional<InvoiceDocument> findByInvoiceNumber(String invoiceNumber);

    Optional<InvoiceDocument> findByOrderId(String orderId);

    List<InvoiceDocument> findBySchoolCodeOrderByInvoiceDateDesc(String schoolCode);

    List<InvoiceDocument> findAllByOrderByInvoiceDateDesc();
}
