package com.example.paymentservice.util;

import com.example.paymentservice.config.RazorpayProperties;
import com.example.paymentservice.exception.PaymentException;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RazorpaySignatureUtil {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final RazorpayProperties razorpayProperties;

    public boolean verifySignature(String orderId, String paymentId, String razorpaySignature) {
        String payload = orderId + "|" + paymentId;
        String generatedSignature = generateSignature(payload, razorpayProperties.getSecret());
        return MessageDigest.isEqual(
                generatedSignature.getBytes(StandardCharsets.UTF_8),
                razorpaySignature.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String generateSignature(String payload, String secret) {
        try {
            Mac hmacSha256 = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            hmacSha256.init(secretKey);
            byte[] hash = hmacSha256.doFinal(payload.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte value : hash) {
                hexString.append(String.format("%02x", value));
            }
            return hexString.toString();
        } catch (Exception exception) {
            throw new PaymentException("Unable to verify Razorpay signature");
        }
    }
}
