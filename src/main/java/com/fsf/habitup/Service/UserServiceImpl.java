package com.fsf.habitup.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.fsf.habitup.DTO.LoginRequest;
import com.fsf.habitup.DTO.OtpRegisterRequest;
import com.fsf.habitup.DTO.OtpVerificationReuest;
import com.fsf.habitup.DTO.RegisterRequest;
import com.fsf.habitup.Enums.AccountStatus;
import com.fsf.habitup.Enums.SubscriptionType;
import com.fsf.habitup.Enums.UserType;
import com.fsf.habitup.Exception.ApiException;
import com.fsf.habitup.Repository.PasswordResetTokenRepository;
import com.fsf.habitup.Repository.UserRepository;
import com.fsf.habitup.entity.PasswordResetToken;
import com.fsf.habitup.entity.User;

@Service
public class UserServiceImpl implements UserService {

    private final OtpService otpService;
    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    @Autowired
    private final PasswordResetTokenRepository tokenRepository;

    @Autowired
    private final JavaMailSender mailSender;

    public UserServiceImpl(AuthenticationManager authenticationManager, JavaMailSender mailSender,
            OtpService otpService, PasswordEncoder passwordEncoder, PasswordResetTokenRepository tokenRepository,
            UserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.mailSender = mailSender;
        this.otpService = otpService;
        this.passwordEncoder = passwordEncoder;
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
    }

    @Override
    public String sendOtp(String email) {
        if (userRepository.findByEmail(email) != null) {
            throw new ApiException("This user is already registered");
        }

        // Generate and send OTP
        otpService.generateAndSendOtp(email);
        return "OTP sent to " + email + ". Please verify before completing registration.";
    }

    @Override
    public String verifyOtpAndCreateUser(OtpRegisterRequest request) {
        OtpVerificationReuest otpRequest = request.getOtpVerificationReuest();
        RegisterRequest registerRequest = request.getRegisterRequest();
        if (!otpService.validateOtp(otpRequest.getEmail(), otpRequest.getOtp())) {
            return "Invalid or expired OTP";
        }

        // Create and save user
        User user = new User();
        user.setName(registerRequest.getName());
        user.setEmail(registerRequest.getEmail());
        user.setJoinDate(LocalDateTime.now());
        user.setDob(registerRequest.getDateOfBirth());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setPhoneNo(registerRequest.getPhoneNumber());
        user.setSubscriptionType(null);
        user.setProfilePhoto(null);
        user.setGender(registerRequest.getGender());

        userRepository.save(user);
        return "Registration successful!";
    }

    @Override
    public String authenticateUser(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail());

        if (user == null) {
            throw new ApiException("User not found");
        }
        authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        return "Authentication successful!";
    }

    @Override
    public User updateUser(String email, User updateUser) {

        User existingUser = userRepository.findByEmail(email);

        if (existingUser == null) {
            throw new ApiException("user not found");
        }

        existingUser.setName(updateUser.getName());
        existingUser.setDob(updateUser.getDob());
        existingUser.setPhoneNo(updateUser.getPhoneNumber());
        existingUser.setGender(updateUser.getGender());
        existingUser.setProfilePhoto(updateUser.getProfilePhoto());

        String message = "updated successfully!!";
        System.out.println(message);
        return existingUser;
    }

    @Override
    public User getUserByEmail(String email) {
        User existingUser = userRepository.findByEmail(email);
        if (existingUser == null) {
            throw new ApiException("user is not found!!");
        }
        return existingUser;
    }

    @Override
    public boolean deleteUser(String email) {
        if (userRepository.findByEmail(email) == null) {
            return false;
        }
        userRepository.deleteByEmail(email);
        return true;
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public boolean sendPasswordResetToken(String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            return false;
        }

        String token = UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusMinutes(15);

        PasswordResetToken resetToken = new PasswordResetToken(token, user, expiryDate);
        tokenRepository.save(resetToken);

        String resetLink = "http://localhost:8080/user/reset-password?token=" + token;
        sendEmail(user.getEmail(), resetLink);

        return true;
    }

    private void sendEmail(String recipientEmail, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipientEmail);
        message.setSubject("Password Reset Request");
        message.setText("Click the link below to reset your password:\n" + resetLink);
        mailSender.send(message);
    }

    @Override
    public boolean resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token);
        if (resetToken == null || resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return false; // Token invalid or expired
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword)); // Encrypt password
        userRepository.save(user);

        // Remove the token after successful reset
        tokenRepository.delete(resetToken);
        return true;
    }

    @Override
    public boolean updateUserType(Long userId, UserType userType) {
        return userRepository.findById(userId).map(user -> {
            Date dobDate = user.getDob(); // Get Date type DOB
            if (dobDate == null) {
                return false; // Can't determine UserType without DOB
            }

            // Convert Date to LocalDate
            LocalDate dob = dobDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            int age = Period.between(dob, LocalDate.now()).getYears();

            UserType determinedType;
            if (age < 18) {
                determinedType = UserType.Child;
            } else if (age < 60) {
                determinedType = UserType.Adult;
            } else {
                determinedType = UserType.Elder;
            }

            user.setUserType(determinedType);
            userRepository.save(user);
            return true;
        }).orElse(false);
    }

    @Override
    public boolean updateAccountStatus(Long userId, AccountStatus accountStatus) {
        return userRepository.findById(userId)
                .map(user -> {
                    if (user.getAccountStatus() == AccountStatus.ACTIVE && accountStatus == AccountStatus.INACTIVE) {
                        user.setAccountStatus(AccountStatus.INACTIVE);
                    } else if (user.getAccountStatus() == AccountStatus.INACTIVE
                            && accountStatus == AccountStatus.ACTIVE) {
                        user.setAccountStatus(AccountStatus.ACTIVE);
                    } else {
                        return false; // No change needed
                    }
                    userRepository.save(user);
                    return true;
                })
                .orElse(false); // Return false if user not found
    }

    @Override
    public boolean updateSubscriptionType(Long userId, SubscriptionType subscriptionType, boolean paymentStatus) {
        return userRepository.findById(userId)
                .map(user -> {
                    if (paymentStatus) {
                        user.setSubscriptionType(SubscriptionType.PREMIUM);
                    } else {
                        user.setSubscriptionType(SubscriptionType.FREE);
                    }
                    userRepository.save(user);
                    return true;
                })
                .orElse(false);
    }

}
