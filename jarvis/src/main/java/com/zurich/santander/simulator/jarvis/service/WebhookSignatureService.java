package com.zurich.santander.simulator.jarvis.service;

import com.zurich.santander.simulator.jarvis.config.JarvisProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;

@Service
public class WebhookSignatureService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final JarvisProperties properties;

    public WebhookSignatureService(JarvisProperties properties) {
        this.properties = properties;
    }

    public String sign(String method, String path, String timestamp, String body) {
        String bodyHash = sha256Hex(body);
        String canonical = method + "\n" + path + "\n" + timestamp + "\n" + bodyHash;
        String signature = hmacSha256Hex(canonical, properties.getWebhookSecret());
        return "v1=" + signature;
    }

    String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return toHex(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to hash callback payload", ex);
        }
    }

    private String hmacSha256Hex(String value, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(key);
            byte[] hmac = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return toHex(hmac);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign callback payload", ex);
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

