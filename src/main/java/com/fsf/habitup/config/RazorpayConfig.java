package com.fsf.habitup.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "razorpay")
public class RazorpayConfig {

    private String keyId;
    private String keySecret;

    /**
     * @return String return the keyId
     */
    public String getKeyId() {
        return keyId;
    }

    /**
     * @param keyId the keyId to set
     */
    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    /**
     * @return String return the keySecret
     */
    public String getKeySecret() {
        return keySecret;
    }

    /**
     * @param keySecret the keySecret to set
     */
    public void setKeySecret(String keySecret) {
        this.keySecret = keySecret;
    }

}
