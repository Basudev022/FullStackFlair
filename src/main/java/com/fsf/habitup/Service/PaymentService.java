package com.fsf.habitup.Service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fsf.habitup.config.RazorpayConfig;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.razorpay.Order;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;

@Service
public class PaymentService {

    @Autowired
    private RazorpayConfig razorpayConfig;

    public String initiatePayment(int amountInRupees) throws RazorpayException {
        return createPaymentOrder(amountInRupees);
    }

    // 2. createPaymentOrder (Generic, abstracted from Razorpay)
    public String createPaymentOrder(int amountInRupees) throws RazorpayException {
        RazorpayClient razorpay = new RazorpayClient(razorpayConfig.getKeyId(), razorpayConfig.getKeySecret());

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInRupees * 100); // Convert to paise
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", "txn_" + System.currentTimeMillis());
        orderRequest.put("payment_capture", true);

        Order order = razorpay.orders.create(orderRequest);
        return order.toString(); // send full order response to frontend
    }

    // 3. verifyPayment (signature verification)
    public boolean verifyPayment(String paymentId, String orderId, String signature) {
        try {
            String data = orderId + "|" + paymentId;
            String secret = razorpayConfig.getKeySecret();

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String generatedSignature = Base64.getEncoder().encodeToString(hash);

            return generatedSignature.equals(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Error verifying payment signature: " + e.getMessage(), e);
        }
    }

    // 4. generatePaymentQRCode (QR code for Razorpay Checkout URL or UPI)
    public BufferedImage generatePaymentQRCode(String data) throws WriterException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.MARGIN, 1);

        BitMatrix bitMatrix = new MultiFormatWriter().encode(data, BarcodeFormat.QR_CODE, 250, 250, hints);

        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, width, height);
        graphics.setColor(Color.BLACK);

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                image.setRGB(i, j, bitMatrix.get(i, j) ? Color.BLACK.getRGB() : Color.WHITE.getRGB());
            }
        }

        return image;
    }

    // 5. processPaymentCallback (Placeholder for webhook or manual handling)
    public void processPaymentCallback(String payload, String razorpaySignature) {
        try {
            // Step 1: Generate the expected signature using your helper method
            String expectedSignature = hmacSHA256(payload, razorpayConfig.getKeySecret());

            // Step 2: Convert Base64 of generated signature (if your hmacSHA256 returns
            // hex, skip this step)
            // Razorpay's signature is in hex format, so no Base64 is needed
            if (expectedSignature.equals(razorpaySignature)) {
                System.out.println("✅ Webhook signature verified.");
                System.out.println("Webhook Payload: " + payload);

                // Step 3: Parse payload (optional, example using org.json)
                JSONObject event = new JSONObject(payload);
                String eventType = event.getString("event");

                switch (eventType) {
                    case "payment.captured" -> System.out.println("📦 Payment Captured");
                    case "payment.failed" -> System.out.println("❌ Payment Failed");
                    default -> System.out.println("⚠️ Unhandled event: " + eventType);
                }
            } else {
                System.err.println("❌ Invalid webhook signature!");
            }

        } catch (JSONException e) {
            System.err.println("Exception while processing webhook: " + e.getMessage());
        }
    }

    // 6. handlePaymentResponse (generic post-payment handling)
    public String handlePaymentResponse(String paymentId) throws RazorpayException {
        RazorpayClient razorpay = new RazorpayClient(razorpayConfig.getKeyId(), razorpayConfig.getKeySecret());
        Payment payment = razorpay.payments.fetch(paymentId);

        // You can access various payment fields here
        String status = payment.get("status");
        if ("captured".equalsIgnoreCase(status)) {
            return "Payment Successful";
        } else if ("failed".equalsIgnoreCase(status)) {
            return "Payment Failed";
        } else {
            return "Payment in Progress or Pending";
        }
    }

    // Utility to convert QR image to byte[]
    public byte[] convertToByteArray(BufferedImage image) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    // HMAC-SHA256 Helper
    private String hmacSHA256(String data, String key) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Error generating HMAC-SHA256 signature: " + e.getMessage(), e);
        }
    }
}