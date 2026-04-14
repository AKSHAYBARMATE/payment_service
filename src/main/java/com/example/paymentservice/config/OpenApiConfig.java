package com.example.paymentservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(InvoiceProperties.class)
public class OpenApiConfig {

    @Bean
    public OpenAPI paymentServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Razorpay Payment Service API")
                        .description("Production-ready payment service APIs for order creation, payment verification, and payment history.")
                        .version("v1")
                        .contact(new Contact()
                                .name("Payment Service Team")
                                .email("support@example.com"))
                        .license(new License()
                                .name("Internal Use")
                                .url("https://example.com/internal")));
    }
}
