package com.fsf.habitup.entity;

import java.util.Date;

import com.fsf.habitup.Enums.PaymentStatus;
import com.fsf.habitup.Enums.RenewalStatus;
import com.fsf.habitup.Enums.SubscriptionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "subscription")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subscriptionId", nullable = false, unique = true)
    private Long subscription_Id;

    @OneToOne
    @JoinColumn(name = "userId", referencedColumnName = "userId", unique = true)
    private User user;

    @Column(name = "price", nullable = false, unique = false)
    private int price;

    @Column(name = "startDate", nullable = false, unique = false)
    private Date startDate;

    @Column(name = "endDate", nullable = false, unique = false)
    private Date endDate;

    @Column(name = "paymentStatus", nullable = false, unique = false)
    private PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "renewalStatus", nullable = false, unique = false)
    private RenewalStatus renewalStatus;

    @Column(name = "createdAt", nullable = false, unique = false)
    private Date createdAt;

    @Column(name = "updatedAt", nullable = false, unique = false)
    private Date updatedAt;

    public SubscriptionType getSubscriptionType() {
        return subscriptionType;
    }

    public void setSubscriptionType(SubscriptionType subscriptionType) {
        this.subscriptionType = subscriptionType;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "subscriptionType", nullable = false)
    private SubscriptionType subscriptionType;

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Long getSubscription_Id() {
        return subscription_Id;
    }

    public void setSubscription_Id(Long subscription_Id) {
        this.subscription_Id = subscription_Id;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public RenewalStatus getRenewalStatus() {
        return renewalStatus;
    }

    public void setRenewalStatus(RenewalStatus renewalStatus) {
        this.renewalStatus = renewalStatus;
    }
}
