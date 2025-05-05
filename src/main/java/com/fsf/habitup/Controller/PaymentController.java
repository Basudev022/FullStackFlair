package com.fsf.habitup.Controller;

import java.awt.image.BufferedImage;
import java.io.IOException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fsf.habitup.Service.PaymentService;
import com.fsf.habitup.Service.SubscriptionService;
import com.fsf.habitup.Service.UserService;
import com.google.zxing.WriterException;
import com.razorpay.RazorpayException;

@RestController
@RequestMapping("/habit/payment")
public class PaymentController {

    private PaymentService paymentService;

    private UserService userService;

    private SubscriptionService subscriptionService;

    public PaymentController(PaymentService paymentService, SubscriptionService subscriptionService,
            UserService userService) {
        this.paymentService = paymentService;
        this.subscriptionService = subscriptionService;
        this.userService = userService;
    }

    // Endpoint to initiate a payment for a specific amount
    @PostMapping("/initiate-payment")
    public ResponseEntity<String> initiatePayment(@RequestParam int amountInRupees) {
        try {
            // Initiate the payment and get the order response
            String orderResponse = paymentService.initiatePayment(amountInRupees);
            return ResponseEntity.ok(orderResponse);
        } catch (RazorpayException e) {
            return ResponseEntity.status(500).body("Error initiating payment: " + e.getMessage());
        }
    }

    // Endpoint to handle the response after payment is made
    @GetMapping("/payment-status")
    public ResponseEntity<String> handlePaymentResponse(@RequestParam String paymentId) {
        try {
            // Call the service to check the payment status
            String status = paymentService.handlePaymentResponse(paymentId);
            return ResponseEntity.ok(status);
        } catch (RazorpayException e) {
            return ResponseEntity.status(500).body("Error fetching payment status: " + e.getMessage());
        }
    }

    // Endpoint to handle Razorpay's webhook callback (e.g., for successful
    // payments)
    @PostMapping("/payment-callback")
    public ResponseEntity<String> handlePaymentCallback(@RequestBody String payload,
            @RequestHeader("X-Razorpay-Signature") String razorpaySignature) {
        try {
            // Process the webhook payload
            paymentService.processPaymentCallback(payload, razorpaySignature);
            return ResponseEntity.ok("Payment Callback Processed Successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error processing payment callback: " + e.getMessage());
        }
    }

    // Endpoint to generate a QR code for payment (e.g., Checkout URL or UPI)
    @GetMapping("/generate-qr")
    public ResponseEntity<byte[]> generatePaymentQRCode(@RequestParam String paymentUrl) throws WriterException {
        try {
            BufferedImage qrCodeImage = paymentService.generatePaymentQRCode(paymentUrl);
            byte[] imageBytes = paymentService.convertToByteArray(qrCodeImage);
            return ResponseEntity.ok(imageBytes); // Return the byte array of the QR code image
        } catch (IOException | WriterException e) {
            // Return the error message as a byte array
            String errorMessage = "Error generating QR code: " + e.getMessage();
            return ResponseEntity.status(500).body(errorMessage.getBytes()); // Convert error message to byte[]
        }
    }

    // Helper method to verify payment signature (usually for front-end to call)
    @PostMapping("/verify-signature")
    public ResponseEntity<String> verifyPaymentSignature(@RequestParam String paymentId, @RequestParam String orderId,
            @RequestParam String signature) {
        boolean isValid = paymentService.verifyPayment(paymentId, orderId, signature);
        if (isValid) {
            return ResponseEntity.ok("Signature Verified");
        } else {
            return ResponseEntity.status(400).body("Invalid Signature");
        }
    }
}
