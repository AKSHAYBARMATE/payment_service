package com.example.paymentservice.util;

import com.example.paymentservice.config.InvoiceProperties;
import com.example.paymentservice.entity.InvoiceDocument;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Component;

@Component
public class InvoicePdfGenerator {

    private final PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private final PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private final InvoiceProperties invoiceProperties;

    public InvoicePdfGenerator(InvoiceProperties invoiceProperties) {
        this.invoiceProperties = invoiceProperties;
    }

    public byte[] generate(InvoiceDocument invoiceDocument) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                drawHeader(content);
                drawSellerAndBuyer(content, invoiceDocument);
                drawInvoiceMeta(content, invoiceDocument);
                drawLineItems(content, invoiceDocument);
                drawFooter(content, invoiceDocument);
            }

            document.save(outputStream);
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to generate invoice PDF", exception);
        }
    }

    private void drawHeader(PDPageContentStream content) throws IOException {
        fillRect(content, 36, 760, 523, 42, 0.09f, 0.17f, 0.32f);
        writeText(content, bold, 18, 48, 785, invoiceProperties.getCompanyName(), 1f, 1f, 1f);
        writeText(content, regular, 10, 48, 770, "Tax Invoice - SaaS Subscription Billing", 0.86f, 0.91f, 0.96f);
    }

    private void drawSellerAndBuyer(PDPageContentStream content, InvoiceDocument invoiceDocument) throws IOException {
        fillRect(content, 36, 625, 252, 120, 0.97f, 0.96f, 0.93f);
        fillRect(content, 307, 625, 252, 120, 0.95f, 0.97f, 0.99f);

        writeText(content, bold, 11, 48, 732, "Supplier", 0.20f, 0.20f, 0.20f);
        writeText(content, regular, 10, 48, 716, invoiceProperties.getCompanyName(), 0.24f, 0.24f, 0.24f);
        writeText(content, regular, 10, 48, 702, invoiceProperties.getCompanyAddressLine1(), 0.24f, 0.24f, 0.24f);
        writeText(content, regular, 10, 48, 688, invoiceProperties.getCompanyAddressLine2(), 0.24f, 0.24f, 0.24f);
        writeText(content, regular, 10, 48, 674, "GSTIN: " + valueOrDash(invoiceProperties.getCompanyGstin()), 0.24f, 0.24f, 0.24f);
        writeText(content, regular, 10, 48, 660, "Support: " + valueOrDash(invoiceProperties.getSupportEmail()), 0.24f, 0.24f, 0.24f);

        writeText(content, bold, 11, 319, 732, "Bill To", 0.20f, 0.20f, 0.20f);
        writeText(content, regular, 10, 319, 716, invoiceDocument.getSchoolName(), 0.24f, 0.24f, 0.24f);
        writeText(content, regular, 10, 319, 702, valueOrDash(invoiceDocument.getBillingAddress()), 0.24f, 0.24f, 0.24f);
        writeText(content, regular, 10, 319, 688, "School Code: " + invoiceDocument.getSchoolCode(), 0.24f, 0.24f, 0.24f);
        writeText(content, regular, 10, 319, 674, "GSTIN: " + valueOrDash(invoiceDocument.getSchoolGstin()), 0.24f, 0.24f, 0.24f);
    }

    private void drawInvoiceMeta(PDPageContentStream content, InvoiceDocument invoiceDocument) throws IOException {
        writeText(content, bold, 11, 48, 608, "Invoice Details", 0.16f, 0.16f, 0.16f);
        writeText(content, regular, 10, 48, 590, "Invoice Number: " + invoiceDocument.getInvoiceNumber(), 0.25f, 0.25f, 0.25f);
        writeText(content, regular, 10, 48, 576, "Invoice Date: " + invoiceDocument.getInvoiceDate(), 0.25f, 0.25f, 0.25f);
        writeText(content, regular, 10, 48, 562, "Due Date: " + invoiceDocument.getDueDate(), 0.25f, 0.25f, 0.25f);
        writeText(content, regular, 10, 300, 590, "Order ID: " + invoiceDocument.getOrderId(), 0.25f, 0.25f, 0.25f);
        writeText(content, regular, 10, 300, 576, "Payment ID: " + valueOrDash(invoiceDocument.getPaymentId()), 0.25f, 0.25f, 0.25f);
        writeText(content, regular, 10, 300, 562, "Place of Supply: Maharashtra (27)", 0.25f, 0.25f, 0.25f);
    }

    private void drawLineItems(PDPageContentStream content, InvoiceDocument invoiceDocument) throws IOException {
        float tableTop = 525;
        fillRect(content, 36, tableTop, 523, 24, 0.13f, 0.24f, 0.42f);
        writeText(content, bold, 10, 48, tableTop + 8, "Description", 1f, 1f, 1f);
        writeText(content, bold, 10, 320, tableTop + 8, "SAC", 1f, 1f, 1f);
        writeText(content, bold, 10, 390, tableTop + 8, "Taxable Value", 1f, 1f, 1f);
        writeText(content, bold, 10, 485, tableTop + 8, "Amount", 1f, 1f, 1f);

        fillRect(content, 36, tableTop - 72, 523, 72, 0.99f, 0.99f, 0.99f);
        writeText(content, regular, 10, 48, tableTop - 20,
                invoiceDocument.getPlanName() + " subscription - " + invoiceDocument.getBillingPeriodLabel(),
                0.20f, 0.20f, 0.20f);
        writeText(content, regular, 10, 320, tableTop - 20, "998314", 0.20f, 0.20f, 0.20f);
        writeText(content, regular, 10, 390, tableTop - 20, formatCurrency(invoiceDocument.getTaxableAmount()), 0.20f, 0.20f, 0.20f);
        writeText(content, regular, 10, 485, tableTop - 20, formatCurrency(invoiceDocument.getTaxableAmount()), 0.20f, 0.20f, 0.20f);

        float totalsTop = tableTop - 120;
        fillRect(content, 307, totalsTop - 108, 252, 108, 0.97f, 0.96f, 0.93f);
        writeText(content, regular, 10, 319, totalsTop - 18, "Taxable Amount", 0.22f, 0.22f, 0.22f);
        writeText(content, regular, 10, 490, totalsTop - 18, formatCurrency(invoiceDocument.getTaxableAmount()), 0.22f, 0.22f, 0.22f);
        writeText(content, regular, 10, 319, totalsTop - 36, "CGST @ " + invoiceDocument.getCgstRate() + "%", 0.22f, 0.22f, 0.22f);
        writeText(content, regular, 10, 490, totalsTop - 36, formatCurrency(invoiceDocument.getCgstAmount()), 0.22f, 0.22f, 0.22f);
        writeText(content, regular, 10, 319, totalsTop - 54, "SGST @ " + invoiceDocument.getSgstRate() + "%", 0.22f, 0.22f, 0.22f);
        writeText(content, regular, 10, 490, totalsTop - 54, formatCurrency(invoiceDocument.getSgstAmount()), 0.22f, 0.22f, 0.22f);
        writeText(content, bold, 11, 319, totalsTop - 82, "Grand Total", 0.10f, 0.10f, 0.10f);
        writeText(content, bold, 11, 486, totalsTop - 82, formatCurrency(invoiceDocument.getTotalAmount()), 0.10f, 0.10f, 0.10f);
    }

    private void drawFooter(PDPageContentStream content, InvoiceDocument invoiceDocument) throws IOException {
        writeText(content, bold, 10, 48, 240, "Amount in words", 0.20f, 0.20f, 0.20f);
        writeText(content, regular, 10, 48, 224,
                amountInWords(invoiceDocument.getTotalAmount()) + " only", 0.25f, 0.25f, 0.25f);
        writeText(content, regular, 10, 48, 188,
                "Notes: " + valueOrDash(invoiceDocument.getNotes()), 0.25f, 0.25f, 0.25f);
        writeText(content, regular, 10, 48, 156,
                "This is a computer generated invoice and does not require a physical signature.", 0.35f, 0.35f, 0.35f);
        writeText(content, regular, 10, 48, 138,
                "For billing support contact " + invoiceProperties.getSupportPhone() + " or " + invoiceProperties.getSupportEmail(),
                0.35f, 0.35f, 0.35f);
    }

    private void fillRect(PDPageContentStream content, float x, float y, float width, float height,
                          float red, float green, float blue) throws IOException {
        content.setNonStrokingColor(red, green, blue);
        content.addRect(x, y, width, height);
        content.fill();
    }

    private void writeText(PDPageContentStream content, PDType1Font font, int size, float x, float y,
                           String value, float red, float green, float blue) throws IOException {
        content.beginText();
        content.setFont(font, size);
        content.setNonStrokingColor(red, green, blue);
        content.newLineAtOffset(x, y);
        content.showText(safe(value));
        content.endText();
    }

    private String safe(String value) {
        return value == null ? "-" : value.replace("\n", " ");
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String formatCurrency(BigDecimal amount) {
        return "INR " + amount.setScale(2, RoundingMode.HALF_UP);
    }

    private String amountInWords(BigDecimal amount) {
        BigDecimal rounded = amount.setScale(0, RoundingMode.HALF_UP);
        return rounded.toPlainString() + " rupees";
    }
}
