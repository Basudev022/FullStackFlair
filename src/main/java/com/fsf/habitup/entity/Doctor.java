package com.fsf.habitup.entity;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.fsf.habitup.Enums.AccountStatus;
import com.fsf.habitup.Enums.DocumentStatus;
import com.fsf.habitup.Enums.Gender;
import com.fsf.habitup.Enums.UserType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "doctor")
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "doctorId", nullable = false, unique = true)
    private Long doctorId;

    @Column(name = "password", nullable = false)
    public String password;

    @Column(name = "doctorName", nullable = false, unique = false)
    private String doctorName;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phoneNo", nullable = false, unique = true)
    private Long phoneNo;

    @Column(name = "gender", nullable = false, unique = false)
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(name = "specialization", nullable = false, unique = false)
    private String specialization;

    @Column(name = "yearsOfExperience", nullable = false, unique = false)
    private byte yearsOfExperience;

    @Column(name = "availabilitySchedule", nullable = false, unique = false)
    private Date availabilitySchedule;

    @Column(name = "consultationFee", nullable = false, unique = false)
    private int consultationFee;

    @Column(name = "ratings", nullable = false, unique = false)
    private float ratings;

    @Column(name = "userType", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private UserType userType;

    @Column(name = "AccountStatus", nullable = false)
    @Enumerated(EnumType.STRING)
    private AccountStatus accountStatus;

    @Column(name = "DocumentStatus", nullable = false)
    @Enumerated(EnumType.STRING)
    private DocumentStatus documentStatus;

    @OneToOne
    @JoinColumn(name = "userId", nullable = false, unique = true)
    private User user;

    @ManyToMany
    @JoinTable(name = "doctor_permissions", joinColumns = @JoinColumn(name = "doctorId"), inverseJoinColumns = @JoinColumn(name = "permissionId"))
    private Set<Permission> permissions = new HashSet<>();

    public AccountStatus getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(AccountStatus accountStatus) {
        this.accountStatus = accountStatus;
    }

    public UserType getUserType() {
        return userType;
    }

    public void setUserType(UserType userType) {
        this.userType = userType;
    }

    public Date getAvailabilitySchedule() {
        return availabilitySchedule;
    }

    public void setAvailabilitySchedule(Date availabilitySchedule) {
        this.availabilitySchedule = availabilitySchedule;
    }

    public int getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(int consultationFee) {
        this.consultationFee = consultationFee;
    }

    public Long getDoctorId() {
        return doctorId;
    }

    public void setDoctor_Id(Long doctorId) {
        this.doctorId = doctorId;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public Long getPhoneNo() {
        return phoneNo;
    }

    public void setPhoneNo(Long phoneNo) {
        this.phoneNo = phoneNo;
    }

    public float getRatings() {
        return ratings;
    }

    public void setRatings(float ratings) {
        this.ratings = ratings;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public byte getYearsOfExperience() {
        return yearsOfExperience;
    }

    public void setYearsOfExperience(byte yearsOfExperience) {
        this.yearsOfExperience = yearsOfExperience;
    }

    public DocumentStatus getDocumentStatus() {
        return documentStatus;
    }

    public void setDocumentStatus(DocumentStatus documentStatus) {
        this.documentStatus = documentStatus;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public void setDoctorId(Long doctorId) {
        this.doctorId = doctorId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}