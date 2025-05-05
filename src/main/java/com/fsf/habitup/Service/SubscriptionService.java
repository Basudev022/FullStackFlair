package com.fsf.habitup.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fsf.habitup.Enums.PaymentStatus;
import com.fsf.habitup.Enums.RenewalStatus;
import com.fsf.habitup.Enums.SubscriptionType;
import com.fsf.habitup.Repository.SubscriptionRepository;
import com.fsf.habitup.Repository.UserRepository;
import com.fsf.habitup.entity.Subscription;
import com.fsf.habitup.entity.User;

@Service
public class SubscriptionService {
    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private UserRepository userRepository;

    // Get subscription by user ID
    public Optional<Subscription> getSubscriptionByUser(Long userId) {
        return subscriptionRepository.findByUser_UserId(userId);
    }

    // Get subscriptions by subscription type
    public List<Subscription> getSubscriptionsByType(SubscriptionType subscriptionType) {
        return subscriptionRepository.findBySubscriptionType(subscriptionType);
    }

    // Create or update a subscription
    public String createOrUpdateSubscription(Subscription subscription) {
        if (subscription.getUser() == null || subscription.getUser().getUserId() == null) {
            return "User information is missing";
        }

        Optional<User> userOpt = userRepository.findById(subscription.getUser().getUserId());

        if (userOpt.isEmpty()) {
            return "User not found";
        }

        User user = userOpt.get();
        Optional<Subscription> existingSubscriptionOpt = subscriptionRepository.findByUser_UserId(user.getUserId());

        Date now = new Date();
        Date endDate = calculateEndDate(subscription.getSubscriptionType());

        if (existingSubscriptionOpt.isPresent()) {
            // Update existing subscription
            Subscription existingSubscription = existingSubscriptionOpt.get();
            existingSubscription.setSubscriptionType(subscription.getSubscriptionType());
            existingSubscription.setStartDate(now);
            existingSubscription.setEndDate(endDate);
            existingSubscription.setPaymentStatus(PaymentStatus.COMPLETED); // You can make this dynamic
            existingSubscription.setRenewalStatus(RenewalStatus.ACTIVE);
            existingSubscription.setPrice(subscription.getPrice());
            existingSubscription.setUpdatedAt(now);
            subscriptionRepository.save(existingSubscription);
            return "Subscription updated successfully";
        } else {
            // Create new subscription
            subscription.setUser(user);
            subscription.setStartDate(now);
            subscription.setEndDate(endDate);
            subscription.setPaymentStatus(PaymentStatus.COMPLETED); // Or set based on payment flow
            subscription.setRenewalStatus(RenewalStatus.ACTIVE);
            subscription.setCreatedAt(now);
            subscription.setUpdatedAt(now);
            subscriptionRepository.save(subscription);
            return "Subscription created successfully";
        }
    }

    // Calculate end date based on subscription type
    private Date calculateEndDate(SubscriptionType subscriptionType) {
        Calendar calendar = Calendar.getInstance();
        if (subscriptionType == SubscriptionType.PREMIUM) {
            calendar.add(Calendar.DAY_OF_MONTH, 21);
        } else if (subscriptionType == SubscriptionType.FREE) {
            calendar.add(Calendar.DAY_OF_MONTH, 5);
        }
        return calendar.getTime();
    }

    // Delete a subscription
    public void deleteSubscription(Long subscriptionId) {
        subscriptionRepository.deleteById(subscriptionId);
    }

    // Get all subscriptions
    public List<Subscription> getAllSubscriptions() {
        return subscriptionRepository.findAll();
    }

    // Check if user has an active subscription
    public boolean hasActiveSubscription(Long userId) {
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByUser_UserId(userId);
        if (subscriptionOpt.isPresent()) {
            Subscription subscription = subscriptionOpt.get();
            Date now = new Date();
            return now.before(subscription.getEndDate()) && subscription.getPaymentStatus() == PaymentStatus.COMPLETED;
        }
        return false;
    }

    // Extend an existing subscription by a number of days
    public Subscription extendSubscription(Long subscriptionId, int additionalDays) {
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findById(subscriptionId);
        if (subscriptionOpt.isPresent()) {
            Subscription subscription = subscriptionOpt.get();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(subscription.getEndDate());
            calendar.add(Calendar.DAY_OF_MONTH, additionalDays);
            subscription.setEndDate(calendar.getTime());
            subscription.setUpdatedAt(new Date());
            return subscriptionRepository.save(subscription);
        } else {
            throw new RuntimeException("Subscription not found");
        }
    }
}