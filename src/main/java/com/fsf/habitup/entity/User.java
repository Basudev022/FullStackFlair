package com.fsf.habitup.entity;

import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fsf.habitup.Enums.AccountStatus;
import com.fsf.habitup.Enums.Gender;
import com.fsf.habitup.Enums.SubscriptionType;
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
import jakarta.persistence.Table;

@Entity
@Table(name = "user")
public class User {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "UserId", unique = true, nullable = false)
	private Long userId;

	@Column(name = "Email", unique = true, nullable = false)
	public String email;

	@Column(name = "Name", unique = false, nullable = false)
	public String name;

	@Column(name = "Password", nullable = false)
	public String password;

	@Column(name = "joinDate", nullable = false)
	private String joinDate;

	@Column(name = "DOB", nullable = false)
	public String dob;

	@Column(name = "phoneNumber", nullable = false, unique = true)
	public Long phoneNo;

	@Column(name = "AccountStatus", nullable = false)
	@Enumerated(EnumType.STRING)
	private AccountStatus accountStatus;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SubscriptionType subscriptionType;

	@Column(name = "ProfilePhoto")
	private String profilePhoto;

	@Column(name = "userType", nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private UserType userType;

	@Column(name = "gender", nullable = false)
	@Enumerated(EnumType.STRING)
	private Gender gender;

	@ManyToMany
	@JoinTable(name = "user_permissions", joinColumns = @JoinColumn(name = "userId"), inverseJoinColumns = @JoinColumn(name = "permissionId"))
	@JsonIgnore
	private Set<Permission> permissions = new HashSet<>();

	public Set<Habit> getHabits() {
		return habits;
	}

	public void setHabits(Set<Habit> habits) {
		this.habits = habits;
	}

	@ManyToMany
	@JoinTable(name = "user_habits", joinColumns = @JoinColumn(name = "userId"), inverseJoinColumns = @JoinColumn(name = "habitId"))
	@JsonManagedReference
	@JsonIgnore
	private Set<Habit> habits = new HashSet<>();

	public void addHabit(Habit habit) {
		this.habits.add(habit);
		habit.getUsers().add(this);
	}

	public AccountStatus getAccountStatus() {
		return accountStatus;
	}

	public void setAccountStatus(AccountStatus accountStatus) {
		this.accountStatus = accountStatus;
	}

	public String getDob() {
		return dob;
	}

	public void setDob(String dob) {
		this.dob = dob;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public String getJoinDate() {
		return joinDate;
	}

	public void setJoinDate(String joinDate) {
		this.joinDate = joinDate;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Long getPhoneNo() {
		return phoneNo;
	}

	public void setPhoneNo(Long phoneNo) {
		this.phoneNo = phoneNo;
	}

	public String getProfilePhoto() {
		return profilePhoto;
	}

	public void setProfilePhoto(String profilePhoto) {
		this.profilePhoto = profilePhoto;
	}

	public SubscriptionType getSubscriptionType() {
		return subscriptionType;
	}

	public void setSubscriptionType(SubscriptionType subscriptionType) {
		this.subscriptionType = subscriptionType;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public UserType getUserType() {
		return userType;
	}

	public void setUserType(UserType userType) {
		this.userType = userType;
	}

	public Set<Permission> getPermissions() {
		return permissions;
	}

	public void setPermissions(Set<Permission> permissions) {
		this.permissions = permissions;
	}

}
