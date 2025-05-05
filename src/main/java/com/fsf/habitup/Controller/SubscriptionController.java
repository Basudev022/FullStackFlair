package com.fsf.habitup.Controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fsf.habitup.Enums.SubscriptionType;
import com.fsf.habitup.Service.SubscriptionService;
import com.fsf.habitup.entity.Subscription;

@RestController
@RequestMapping("/habit/subscriptions")
public class SubscriptionController {

    @Autowired
    private SubscriptionService subscriptionService;

    // Create or update a subscription
    @PreAuthorize("hasAuthority('MANAGE_SUBSCRIPTIONS')")
    @PostMapping("/create-or-update")
    public ResponseEntity<String> createOrUpdateSubscription(@RequestBody Subscription subscription) {
        String result = subscriptionService.createOrUpdateSubscription(subscription);
        return ResponseEntity.ok(result);
    }

    // Get subscription by user ID
    @PreAuthorize("hasAuthority('VIEW_SUBSCRIPTIONS')")
    @GetMapping("/user/{userId}")
    public ResponseEntity<Subscription> getSubscriptionByUser(@PathVariable Long userId) {
        Optional<Subscription> subscription = subscriptionService.getSubscriptionByUser(userId);
        return subscription.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // Get subscriptions by subscription type
    @PreAuthorize("hasAuthority('VIEW_SUBSCRIPTIONS')")
    @GetMapping("/type/{subscriptionType}")
    public ResponseEntity<List<Subscription>> getSubscriptionsByType(@PathVariable SubscriptionType subscriptionType) {
        List<Subscription> subscriptions = subscriptionService.getSubscriptionsByType(subscriptionType);
        return ResponseEntity.ok(subscriptions);
    }

    // Delete subscription by ID
    @PreAuthorize("hasAuthority('MANAGE_SUBSCRIPTIONS')")
    @DeleteMapping("/delete/{subscriptionId}")
    public ResponseEntity<Void> deleteSubscription(@PathVariable Long subscriptionId) {
        subscriptionService.deleteSubscription(subscriptionId);
        return ResponseEntity.noContent().build();
    }

    // Get all subscriptions
    @PreAuthorize("hasAuthority('VIEW_SUBSCRIPTIONS')")
    @GetMapping("/get-all")
    public ResponseEntity<List<Subscription>> getAllSubscriptions() {
        return ResponseEntity.ok(subscriptionService.getAllSubscriptions());
    }

    // Check if a user has an active subscription
    @PreAuthorize("hasAuthority('VIEW_SUBSCRIPTIONS')")
    @GetMapping("/user/{userId}/active")
    public ResponseEntity<Boolean> hasActiveSubscription(@PathVariable Long userId) {
        boolean hasActive = subscriptionService.hasActiveSubscription(userId);
        return ResponseEntity.ok(hasActive);
    }

    // Extend an existing subscription
    @PreAuthorize("hasAuthority('MANAGE_SUBSCRIPTIONS')")
    @PutMapping("/extend/{subscriptionId}")
    public ResponseEntity<Subscription> extendSubscription(@PathVariable Long subscriptionId,
            @RequestParam int additionalDays) {
        try {
            Subscription extendedSubscription = subscriptionService.extendSubscription(subscriptionId, additionalDays);
            return ResponseEntity.ok(extendedSubscription);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}