package com.example.paymentservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.invoice")
public class InvoiceProperties {

    private String companyName;
    private String companyGstin;
    private String companyAddressLine1;
    private String companyAddressLine2;
    private String supportEmail;
    private String supportPhone;
}
