package com.example.paymentservice.controller;

import com.example.paymentservice.dto.ApiResponse;
import com.example.paymentservice.dto.InvoiceResponse;
import com.example.paymentservice.service.InvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@Tag(name = "Invoice APIs", description = "Download and inspect Indian GST invoice documents for school subscriptions")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @GetMapping
    @Operation(summary = "Get all invoices", description = "Returns invoice list for all schools.")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getAllInvoices() {
        return ResponseEntity.ok(ApiResponse.success("Invoices fetched successfully", invoiceService.getAllInvoices()));
    }

    @GetMapping("/school/{schoolCode}")
    @Operation(summary = "Get invoices by school", description = "Returns all invoices for a given school.")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getInvoicesBySchool(@PathVariable String schoolCode) {
        return ResponseEntity.ok(ApiResponse.success("School invoices fetched successfully", invoiceService.getInvoicesBySchoolCode(schoolCode)));
    }

    @GetMapping("/{invoiceNumber}")
    @Operation(summary = "Get invoice details", description = "Returns invoice metadata for display in the admin UI.")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoice(@PathVariable String invoiceNumber) {
        return ResponseEntity.ok(ApiResponse.success("Invoice fetched successfully", invoiceService.getInvoice(invoiceNumber)));
    }

    @GetMapping("/{invoiceNumber}/download")
    @Operation(summary = "Download invoice PDF", description = "Downloads the generated GST-compliant invoice PDF for a school subscription.")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable String invoiceNumber) {
        byte[] pdf = invoiceService.downloadInvoicePdf(invoiceNumber);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + invoiceNumber + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
